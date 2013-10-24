(ns 
  ^{:doc "Relations between JDT ASTNodes and SOOT whole-program analyses."
    :author "Coen De Roover"}
  damp.ekeko.jdt.soot
  (:refer-clojure :exclude [== type])
  (:use [clojure.core.logic])
  (:use [damp.ekeko logic])
  (:use [damp.ekeko.jdt reification basic])
  (:require [damp.ekeko.jdt [astnode :as astnode]]
            [damp.ekeko [ekekomodel :as ekekomodel]]
            [damp.ekeko.soot [soot :as soot]])
  (:import 
    [java.util Iterator]
    [damp.ekeko JavaProjectModel EkekoModel]
    [damp.ekeko.soot SootProjectModel]
    [soot Context MethodOrMethodContext PointsToAnalysis SootMethodRef Local Scene PointsToSet SootClass SootMethod SootField Body Unit ValueBox Value PatchingChain]
    [soot.jimple JimpleBody StaticFieldRef InstanceFieldRef]
    [soot.tagkit SourceLnPosTag LineNumberTag]
    [soot.jimple.internal JAssignStmt JimpleLocal JInstanceFieldRef JIdentityStmt]
    [soot.toolkits.graph ExceptionalUnitGraph]
    [soot.jimple.toolkits.callgraph Edge CallGraph]
    [soot.jimple.toolkits.pointer StrongLocalMustAliasAnalysis LocalMustNotAliasAnalysis LocalMustAliasAnalysis]
    [org.eclipse.jdt.core.dom IBinding VariableBinding ASTNode$NodeList VariableDeclarationFragment FieldDeclaration FieldAccess QualifiedName IVariableBinding SimpleName ITypeBinding Expression ASTNode CompilationUnit]
    ))


;; SOOT <-> JDT
;; ------------


(defn 
  method-declaration-signature
  "Relation between the JDT MethodDeclaration ?ast
   and its Soot signature string ?signature."
  [?ast ?signature]
   (all
    (ast :MethodDeclaration ?ast)
    (equals ?signature (JavaProjectModel/keyForMethodDeclaration ?ast))))

(defn 
  type-declaration-signature 
  "Relation between the JDT TypeDeclaration ?ast
   and its Soot signature string ?signature."
  [?ast ?signature]
  (all
    (ast :TypeDeclaration ?ast)
    (equals ?signature (JavaProjectModel/keyForTypeDeclaration ?ast))))

(defn 
  field-signature
  "Relation between the JDT FieldDeclaration ?ast
   and its Soot signature string ?signature."
  [?ast ?signature]
  (fresh [?declaration]
         (ast :FieldDeclaration ?declaration)
         (child :fragments ?declaration ?ast)
         (equals ?signature (JavaProjectModel/sootSignatureForFieldVariableBinding (.resolveBinding ^VariableDeclarationFragment ?ast)))))

(defn 
  ast-soot-class
  "Relation between the JDT TypeDeclaration ?jdt
   and the corresponding SootClass ?soot."
  [?jdt ?soot]
    (fresh [?model ?scene ?binaryname]
           (soot/soot-model-scene ?model ?scene)
           (ast-model :TypeDeclaration ?jdt ?model) 
           (type-declaration-signature ?jdt ?binaryname)
           (soot/soot-class-name ?soot ?binaryname)))


(def 
 #^{:doc 
    "Relation between the JDT MethodDeclaration ?jdt
     and the corresponding SootMethod ?soot.
     Tabled."}
  ast-soot-method 
  (tabled
    [?jdt ?soot]
    (fresh [?model ?scene ?signature]
           (soot/soot-model-scene ?model ?scene)
           (ast-model :MethodDeclaration ?jdt ?model) 
           (method-declaration-signature ?jdt ?signature)
           (soot/soot-method-signature ?soot ?signature))))

(defn 
  ast-soot-field 
  "Relation between the JDT FieldDeclaration ?declaration,
   one of its VariableDeclarationFragment instances ?field,
   and the corresponding SootField ?soot.

  Note that a field declaration AST node can declare multiple fields."
  [?declaration ?field ?soot]
    (fresh [?model ?scene ?signature]
           (soot/soot-model-scene ?model ?scene)
           (ast-model :FieldDeclaration ?declaration ?model) 
           (child :fragments ?declaration ?field)
           (field-signature ?field ?signature)
           (soot/soot-field-signature ?soot ?signature)))

;; JDT Expressions -> SOOT Unit, Locals
;; ------------------------------------

(defn-
  ast-unit-position-compatible?
  [ast unit]
  (let [root (.getRoot ^ASTNode ast)
        astStart (.getStartPosition ^ASTNode ast)
        astEnd (+ astStart (.getLength ^ASTNode ast))
        astLineStart (.getLineNumber ^CompilationUnit root astStart)
        astLineEnd (.getLineNumber ^CompilationUnit root astEnd)
        pos (.getTag ^Unit unit "SourceLnPosTag")]
    (if pos
      (let [ulStart (.startLn ^SourceLnPosTag pos)
            ulEnd (.endLn ^SourceLnPosTag pos)]
        (and (<= astLineStart ulStart)
             (>= astLineEnd ulEnd)))
      (let [pos2 (.getTag ^Unit unit "LineNumberTag")]
        (if pos2
          (let [line (.getLineNumber ^LineNumberTag pos2)]
            (and (<= astLineStart line)
                 (>= astLineEnd line)))
          true ;can only be used to weed incompatible ones out
          )))))

(defn 
  is-ast-soot-unit-position-compatible?
  "Relation of JDT ASTNode instances ?ast and Soot Unit instances ?unit
   that are compatible based on their respective line number information. "
  [?ast ?unit] 
  (fresh [?astkeyw ?unitkeyw]
    (ast ?astkeyw ?ast)
    (soot/soot-unit ?unitkeyw ?unit)))

;todo: arrays
(defn 
  ast-reference-soot-model
  "Relation between reference-valued JDT Expression instances ?ast
   and the Ekeko model ?model in which they reside."
  [?ast ?model]
  (fresh [?scene ?keyw ?binding]
         (soot/soot-model-scene ?model ?scene)
         (ast-model :Expression ?ast ?model)
         (ast-expression-reference ?keyw ?ast)))
         
         

(defn 
  ast-reference-soot-model-method
  "Relation between a reference-valued JDT Expression ?ast,
   the Ekeko model ?model in which it resides,
   and the SootMethod ?sm in which the corresponding Soot
   Unit is to be found.

   Note that ?sm can be a class initializer as well as
   a constructor."
  [?ast ?model ?sm]
    (all
      (ast-reference-soot-model ?ast ?model)
      (conde [(fresh [?m]
                     (ast-encompassing-method ?ast ?m)
                     (ast-soot-method ?m ?sm))]
             [(fresh [?p ?t ?sc]
                     (ast-parent ?ast ?p)
                   (ast :FieldDeclaration ?p)
                   (ast-encompassing-typedeclaration ?ast ?t)
                   (ast-soot-class ?t ?sc)
                   (conde [(fresh [?mod]
                                  (ast-declaration-modifier :FieldDeclaration ?ast ?mod) 
                                  (modifier-static ?mod))
                           (soot/soot-class-initializer-method ?sc ?sm)]
                          [(soot/soot-class-constructor-method ?sc ?sm)]))])))

;TODO: assumes SOOT has been run with variable name preservation option on (was not the case for SOUL's Cava)
(defn 
  is-ast-soot-name-local-compatible?
  "Non-relational. Verifies that JDT SimpleName instance ?name
   has the same identifier as Soot JimpleLocal ?local."
  [?name ?local]
  (fresh [?identifier]
         (ast :SimpleName ?name)
         (soot/soot-value :JimpleLocal ?local)
         (equals ?identifier (.getName ^JimpleLocal ?local))
         (equals ?identifier (.getIdentifier ^SimpleName ?name))))

(defn 
  is-ast-soot-value-type-compatible? 
  "Non-relational. Verifies that the declared type of JDT Expression instance ?expression
   and the declared type of Soot Value ?value have the same Soot signature string."
  [?expression ?value]  
 (fresh [?signature]
        (equals ?signature (JavaProjectModel/sootSubsignatureTypeStringForEclipseTypeBinding (.resolveTypeBinding ^Expression ?expression)))
        (equals ?signature (.toString ^soot.Type (.getType ^Value ?value)))))


(defn 
  is-soot-local-named-this?
  "Non-relational. Verifies that Soot JimpleLocal ?local
   is named 'this'."
  [?local]
  (all 
    (soot/soot-value :JimpleLocal ?local)
    (equals "this" (.getName ^JimpleLocal ?local))))


(defn 
  is-ast-invocation-soot-value-compatible-name-args?
  "Non-relational. Verifies that the names and argument counts of invocation-like
   JDT Expression instance ?expression and SootMethodRef ?value are compatible."
  [?expression ?value]
  ;TODO: provide type hints based on it being one of the method invocations (which only have Expresison as common super type(
  (fresh [?identifier]
          (equals ?identifier (.getIdentifier ^SimpleName (.getName ?expression)))
          (equals ?identifier (.name ^SootMethodRef (.getMethodRef ?value)))
          (fresh [?argcnt]
                 (equals ?argcnt (.size ^ASTNode$NodeList (.arguments ?expression))) 
                 (equals ?argcnt (.getArgCount ?value)))))

(defn 
  is-ast-expression-soot-field-access-compatible-local-on-lhs?
    "Non-relational. Verifies that JDT Expression ?expression of kind ?expressionKeyw 
     that accesses a field with binding ?fieldBinding, can feature on the RHS of
     a Soot Unit with SootValue ?value on its LHS of which ?valKeyw represents the kind."
  [?fieldBinding ?expressionKeyw ?expression ?valKeyw ?value]
  (fresh [?modFlags ?sig ?sf]
         (equals ?sig (JavaProjectModel/sootSignatureForFieldVariableBinding ^IVariableBinding ?fieldBinding))
         (!= ?sig nil)
         (soot/soot-field-signature ?sf ?sig)
         (equals ?modFlags (.getModifiers ^VariableBinding ?fieldBinding)) ;TODO: add type hint to JDT-internal IFieldVariableBinding (or something like that)
         (conda [(succeeds (org.eclipse.jdt.core.dom.Modifier/isStatic ?modFlags)) 
                 (== ?valKeyw :StaticFieldRef)
                 (equals ?sf (.getField ^StaticFieldRef ?value))]
                [(== ?valKeyw :JInstanceFieldRef)
                  (equals ?sf (.getField ^InstanceFieldRef ?value))
                  (fresh [?base]
                         (equals ?base (.getBase ^InstanceFieldRef ?value))
                         (conda [(== ?expressionKeyw :SimpleName)
                                 (is-soot-local-named-this? ?base)]
                                [(soot/soot-value :JimpleLocal ?base)]))])))


;TODO: this was much more refined and precise in Cava
;assignment unit where expression is on the RHS, local on the LHS
(defn 
  is-ast-expression-soot-value-compatible-local-on-lhs?
  "Non-relational. Verifies that JDT Expression ?expression of which ?keyw
   represents the kind, can feature on the RHS of a Soot Unit 
   with SootValue ?value on its LHS of which ?valKeyw represents the kind."
  [?keyw ?expression ?valkeyw ?value]
  (conda [(== ?keyw :ClassInstanceCreation)
          (== ?valkeyw :JNewExpr)
          ;todo: check arg count of init invocation in trailing unit
          ]       
         [(== ?keyw :CastExpression)
          (== ?valkeyw :JCastExpr)]
         [(== ?keyw :MethodInvocation)
          (all
            (conda [(== ?valkeyw :JStaticInvokeExpr)]
                   [(== ?valkeyw :JVirtualInvokeExpr)]
                   [(== ?valkeyw :JInterfaceInvokeExpr)])
            (is-ast-invocation-soot-value-compatible-name-args? ?expression ?value))]
         [(== ?keyw :SuperMethodInvocation) 
          (== ?valkeyw :JSpecialInvokeExpr) ;todo: qualified super invocations (cf Cava)
          (is-ast-invocation-soot-value-compatible-name-args? ?expression ?value)]
         [(== ?keyw :ThisExpression)
          fail ;better to take an identitystmt unit with this local on the lhs
          (is-soot-local-named-this? ?value)] ;todo qualified this expressions (cf Cava)
         [(== ?keyw :FieldAccess) ;TODO: what's the Jimple equivalent of a SuperFieldAccess?
          (fresh [?binding]
                 (equals ?binding (.resolveFieldBinding ^FieldAccess ?expression))
                 (is-ast-expression-soot-field-access-compatible-local-on-lhs? ?binding ?keyw ?expression ?valkeyw ?value))]
         [(== ?keyw :QualifiedName)
           (fresh [?binding]
                 (equals ?binding (.resolveBinding ^QualifiedName ?expression))
                 (is-ast-expression-soot-field-access-compatible-local-on-lhs? ?binding ?keyw ?expression ?valkeyw ?value))]
         [(== ?keyw :SimpleName)
          (fresh [?nameBinding]
                 (equals ?nameBinding (.resolveBinding ^SimpleName ?expression))
                 (conda [(all 
                           (succeeds (instance? IVariableBinding ?nameBinding)) ; can also resolve to a type binding
                           (succeeds (.isField ^IVariableBinding ?nameBinding))) 
                         (is-ast-expression-soot-field-access-compatible-local-on-lhs? ?nameBinding ?keyw ?expression ?valkeyw ?value)]
                        [;local on rhs 
                         (== ?valkeyw :JimpleLocal)
                         fail ;better to take a unit with this local on the lhs (TODO: SURE? NOT THE CASE FOR FLOW-SENSITIVE ANALYSES)
                         (is-ast-soot-name-local-compatible? ?expression ?value)]
                        ))]
         ;TODO:expand to array accesses
         ))

          
          
(defn 
  is-ast-expression-soot-field-access-compatible-local-on-rhs? 
  "Non-relational. Verifies that JDT Expression ?expression of kind ?expressionKeyw 
   that accesses a field with binding ?fieldBinding, can feature on the LHS of
   a Soot Unit with SootValue ?value on its RHS of which ?valKeyw represents the kind."
  [?fieldBinding ?expressionKeyw ?expression ?valKeyw ?value]
  (fresh [?modFlags ?sig ?sf]
         (equals ?sig (JavaProjectModel/sootSignatureForFieldVariableBinding ^IVariableBinding ?fieldBinding))
         (!= ?sig nil)
         (soot/soot-field-signature ?sf ?sig)
         (equals ?modFlags (.getModifiers ^VariableBinding ?fieldBinding)) 
         (conda [(succeeds (org.eclipse.jdt.core.dom.Modifier/isStatic ?modFlags))
                 (== ?valKeyw :StaticFieldRef)
                 (equals ?sf (.getField ^StaticFieldRef ?value))]
                [(== ?valKeyw :JInstanceFieldRef)
                  (equals ?sf (.getField ^JInstanceFieldRef ?value))
                  (fresh [?base]
                         (equals ?base (.getBase ^JInstanceFieldRef ?value))
                         (conda [(== ?expressionKeyw :SimpleName)
                                 (is-soot-local-named-this? ?base)]
                                [(soot/soot-value :JimpleLocal ?base)]))])))



;TODO: this was much more refined and precise in Cava
;assignment unit where expression is on the LHS, local on the RHS
(defn 
  is-ast-expression-soot-value-compatible-local-on-rhs? 
  "Non-relational. Verifies that JDT Expression ?expression of kind ?expressionKeyw 
   that accesses a field with binding ?fieldBinding, can feature on the LHS of
   a Soot Unit with SootValue ?value on its RHS of which ?valKeyw represents the kind."
  [?keyw ?expression ?lvalkeyw ?lvalue]
    (conda [(== ?keyw :SimpleName) 
            (is-ast-soot-name-local-compatible? ?expression ?lvalue)]
           [(== ?keyw :FieldAccess)
            (fresh [?binding]
                 (equals ?binding (.resolveFieldBinding ^FieldAccess ?expression))
                 (is-ast-expression-soot-field-access-compatible-local-on-rhs? ?binding ?keyw ?expression ?lvalkeyw ?lvalue))]
           [(== ?keyw :QualifiedName)
             (fresh [?binding]
                 (equals ?binding (.resolveBinding ^QualifiedName ?expression))
                 (is-ast-expression-soot-field-access-compatible-local-on-rhs? ?binding ?keyw ?expression ?lvalkeyw ?lvalue)
                 )]      
           ;TODO:expand to array indices being assigned to
    ))
                                        
    
;assignment unit where expression is on the RHS, local on the LHS
(defn 
  is-ast-unit-assign-compatible-local-on-lhs?
  "Non-relational. Verifies that ?unit is a Soot assignment Unit
   with a JimpleLocal on the LHS and a Soot value compatible with
   JDT ASTNode ?expression on its RHS."  
  [?expression ?unit]
  (fresh [?keyw ?valkeyw ?left ?right ?signature]
    (ast ?keyw ?expression)
    (soot/soot-unit-assign-leftop ?unit ?left)
    (soot/soot-value :JimpleLocal ?left)
    (soot/soot-unit-assign-rightop ?unit ?right)
    (soot/soot-value ?valkeyw ?right)    
    (is-ast-expression-soot-value-compatible-local-on-lhs? ?keyw ?expression ?valkeyw ?right))) 
    
;assignment unit where expression is on the LHS, local on the RHS
;NOTE that this is only correct for flow-insensitive analyses 
(defn 
  is-ast-unit-assign-compatible-local-on-rhs? 
  "Non-relational. Verifies that ?unit is a Soot assignment Unit
   with a JimpleLocal on the RHS and a Soot value compatible with
   JDT ASTNode ?expression on its LHS."
  [?expression ?unit]
  (fresh [?keyw ?lvalkeyw ?left ?right ?signature]
    (ast ?keyw ?expression)
    (soot/soot-unit-assign-rightop ?unit ?right)
    (soot/soot-value :JimpleLocal ?right)
    (soot/soot-unit-assign-leftop ?unit ?left)    
    (soot/soot-value ?lvalkeyw ?left)    
    (is-ast-expression-soot-value-compatible-local-on-rhs? ?keyw ?expression ?lvalkeyw ?left))) 

  
(defn 
  is-ast-unit-identity-or-assignment-compatible-local-on-lhs?
  "Non-relational. Verifies that ?unit is a Soot JIdentityStmt or JAssignStmt
   with a JimpleLocal on its LHS that is compatible with the SimpleName or
   ThisExpression ?expression."
  [?expression ?unitkeyw ?unit]
  (fresh [?keyw ?local]
         (conda [(== ?unitkeyw :JIdentityStmt) (equals ?local (.getLeftOp ^JIdentityStmt ?unit))]
                [(== ?unitkeyw :JAssignStmt) (equals ?local (.getLeftOp ^JAssignStmt ?unit))])
         (ast ?keyw ?expression)
         (conda [(== ?keyw :SimpleName)
                 ;local varibale declaration, parameter names
                 (is-ast-soot-name-local-compatible? ?expression ?local)]
                ;initial assignment of this
                [(== ?keyw :ThisExpression)
                 (is-soot-local-named-this? ?local)]
                ))) 
         

;TODO: how can tabling persist across run* invocations?
(def 
  #^{:doc  
   "Relation between a JDT reference-valued Expression ?expression,
   the Ekeko model ?model in which it resides,
   its corresponding SootUnit ?unit and Local ?local. 

   Tabled.

   Using ?unit and ?local, the Soot program analyses in ?model
   can be queried about the results for ?expression."}
  ast-reference-model-soot-unit-local
  (tabled 
    [?expression ?model ?unit ?local]
    (fresh [?m ?units ?value]
           (ast-reference-soot-model-method ?expression ?model ?m)
           (soot/soot-method-units ?m ?units)
           (contains ?units ?unit)
           (is-ast-soot-unit-position-compatible? ?expression ?unit)
           ;this conde cannot be changed into a conda without getting errors (TODO: check invariants that govern use of conda)
           (conde [;identity or assigment unit assigning to an expression that is a local name
                   (conda [(ast :SimpleName ?expression)]
                          [(ast :ThisExpression ?expression)])
                   (fresh [?unitKeyw]
                          (soot/soot-unit ?unitKeyw ?unit)
                          ;silly conda to enable type hinting
                          (conda [(== ?unitKeyw :JIdentityStmt) (equals ?local (.getLeftOp ^JIdentityStmt ?unit))]
                                 [(== ?unitKeyw :JAssignStmt) (equals ?local (.getLeftOp ^JAssignStmt ?unit))])
                          (is-ast-unit-identity-or-assignment-compatible-local-on-lhs? ?expression ?unitKeyw ?unit))]
                  [ ;assignment unit where expression is on the RHS, local on the LHS
                   (is-ast-unit-assign-compatible-local-on-lhs? ?expression ?unit)
                   (soot/soot-unit-assign-leftop ?unit ?local)
                   ;TODO: Cava used to check wjether a subsequent unit uses the local as the expression's parent
                   ;(fresh [?trailingUnit]
                   ;       (soot-method-unit-trailing-unit ?m ?unit ?trailingUnit))
                   ]
                  [
                   ;assignment unit where expression on the LHS, local is on the RHS
                   (is-ast-unit-assign-compatible-local-on-rhs? ?expression ?unit)
                   (soot/soot-unit-assign-rightop ?unit ?local)]
                  )
           (is-ast-soot-value-type-compatible? ?expression ?local)
           )))
            
            


           
           ;(conda [(ast :ClassInstanceCreation ?expression) 
           ;        (ast-soot-expression-class-instance-creation ?expression ?unit ?local)])))




;following qwal-query does not work
;
;(defn ast-soot-expression-class-instance-creation [?exp ?unit ?local]
;  (fresh [?m ?cfg ?entry ?exit 
;          ?model ?scene ?signature]
;         (soot-model-scene ?model ?scene)
;         (ast-model :ClassInstanceCreation ?exp ?model)
;         (ast-expression-soot-unit-in-method ?exp ?m)
;         (soot-method-cfg ?m ?cfg)
;         (soot-method-cfg-entry ?m ?cfg ?entry)
;        (soot-method-cfg-exit ?m ?cfg ?exit)
;         (project [?cfg]  
;                  (qwal ?cfg ?entry ?exit 
;                        []
;                        (q=>*)
;                        (with-current [u] 
;                          (fresh [?foo ?value]
;                                 (== ?unit u)
;                                 (soot-unit-assign-rightop ?unit ?value)
;                                 (soot-value :JNewExpr ?value)
;                                 (soot-unit-assign-leftop ?unit ?local)))
;                        ;(q=>+)
                        ;(with-current [u]
                        ;  (fresh [?vt ?t]
                         ;        (== ?t u)
                          ;       (soot-unit :JInvokeStmt ?t)
                           ;      (equals ?vt (.getInvokeExpr ?t))))
;                        (q=>+)))))


;; JDT ALiasing
;; ------------

;assuming the geometric encoding pta was run
;(defn ast-reference-soot-model-points-to-context [?ast ?model ?set ?sm]
;  (fresh [?scene ?unit ?local ?sm ?context]
;         (soot-model-scene ?model ?scene)
;         (ast-reference-model-soot-unit-local ?ast ?model ?unit ?local)
;         (ast-reference-soot-model-method ?ast ?model ?sm)
;         (soot-method-called-by-unit ?sm ?context)
;         (equals ?set (.reachingObjects 
;                        ^PointsToAnalysis (.getPointsToAnalysis ^Scene ?scene) 
;                        ^Context ?context
;                        ^Local ?local))))




;TODO: following gives an #<IllegalArgumentException java.lang.IllegalArgumentException: No implementation of method: :ifu of protocol: #'clojure.core.logic/IIfU found for class: clojure.lang.PersistentVector>
;check out why .. as all variables should already by bound before the call to condu
;(defn ast-references-soot-model-may-alias-nodup
;  [?ast1 ?ast2 ?model]
;  (fresh [?foo]
;    (ast-reference-soot-model ?ast1 ?model)
;    (ast-reference-soot-model ?ast2 ?model)
;    (condu 
;      [(ast-references-soot-model-may-alias ?ast1 ?ast2 ?model) succeed])))
  
 

;assuming the geometric encoding pta was run v (TODO: perform instance? check omn pta)
;(defn ast-references-soot-model-may-alias-context
;  [?ast1 ?ast2 ?model ?context]
;  (fresh [?set1 ?set2]
;    (ast-reference-soot-model-points-to ?ast1 ?model ?set1 ?context) ;if given to one (a condu), ensure that ?model is already bound or remains unbound
;    (ast-reference-soot-model-points-to ?ast2 ?model ?set2 ?context)
;    (succeeds (.hasNonEmptyIntersection ^PointsToSet ?set1 ?set2))))



(def 
  #^{:doc 
  "Relation between a reference-valued JDT ASTNode ?ast, 
   the Ekeko model in which it resides,
   and the points-to set ?set of object approximations it might evaluate to
   at run-time.
   Tabled."}
  ast-reference-soot-model-points-to
  (tabled 
    [?ast ?model ?set]
    (fresh [?scene ?unit ?local]
           (ast-reference-model-soot-unit-local ?ast ?model ?unit ?local)
           (soot/soot-model-scene ?model ?scene)
         (equals ?set (.reachingObjects ^PointsToAnalysis (.getPointsToAnalysis ^Scene ?scene) ^Local ?local)))))

;TODO: try to use one as it only only has to succeed for one non-empty intersection, but impossible to use since ?set1 and ?set2 will be bound
(defn 
  ast-references-soot-model-may-alias
  "Relation of two reference-valued JDT ASTNode instances ?ast1 and ?ast2
   that may alias each other (i.e., have a non-empty intersection of points-to sets)
   and the model ?model in which they reside.


   Examples:
   (ekeko-n* 100
             [?ast1 ?ast2]
             (fresh [?model]
                    (ast-references-soot-model-may-alias ?ast1 ?ast2 ?model)))
   "
  [?ast1 ?ast2 ?model]
  (fresh [?set1 ?set2]
    (ast-reference-soot-model-points-to ?ast1 ?model ?set1) ;if given to one (a condu), ensure that ?model is already bound or remains unbound
    (ast-reference-soot-model-points-to ?ast2 ?model ?set2)
    (succeeds (.hasNonEmptyIntersection ^PointsToSet ?set1 ?set2)))) ;assuming the geometric encoding pta was run

(defn 
  ast-references-soot-model-local-must-alias
  "Relation of two reference-valued JDT ASTNode instances
   ?ast1 and ?ast2 in the same method, 
   that must alias according to the method's intra-procedural must alias analysis, 
   and the model ?model in which they reside."
  [?ast1 ?ast2 ?model]
  (fresh [?m ?unit1 ?unit2 ?local1 ?local2 ?a]
         (ast-reference-soot-model-method ?ast1 ?model ?m)
         (ast-reference-soot-model-method ?ast2 ?model ?m) ;TODO: make called predicate mode-aware
         (soot/soot-method-local-must-alias-analysis ?m ?a)
         (ast-reference-model-soot-unit-local ?ast1 ?model ?unit1 ?local1)
         (ast-reference-model-soot-unit-local ?ast2 ?model ?unit2 ?local2) 
         (succeeds (.mustAlias ^LocalMustAliasAnalysis ?a ?local1 ?unit1 ?local2 ?unit2))))

(defn 
  ast-references-soot-model-local-must-alias-strong
  "Relation of two reference-valued JDT ASTNode instances
   ?ast1 and ?ast2 in the same method, 
   that must alias according to the method's 
   strong intra-procedural must alias analysis, 
   and the model ?model in which they reside."
  [?ast1 ?ast2 ?model]
  (fresh [?m ?unit1 ?unit2 ?local1 ?local2 ?a]
         (ast-reference-soot-model-method ?ast1 ?model ?m)
         (ast-reference-soot-model-method ?ast2 ?model ?m) ;TODO: make called predicate mode-aware
         (soot/soot-method-local-must-alias-analysis-strong ?m ?a)
         (ast-reference-model-soot-unit-local ?ast1 ?model ?unit1 ?local1)
         (ast-reference-model-soot-unit-local ?ast2 ?model ?unit2 ?local2) 
         (succeeds (.mustAlias  ^StrongLocalMustAliasAnalysis ?a ?local1 ?unit1 ?local2 ?unit2))))

  


              