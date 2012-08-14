(ns 
  ^{:doc "Relations between JDT ASTNodes and SOOT whole-program analyses."
    :author "Coen De Roover"}
  damp.ekeko.jdt.soot
  (:refer-clojure :exclude [==])
  (:use [clojure.core.logic])
  (:use [damp.ekeko logic])
  (:use [damp.ekeko.jdt reification basic])
  (:require [damp.ekeko.jdt [astnode :as astnode]])
  (:require [damp.ekeko [ekekomodel :as ekekomodel]])
  (:import 
    [java.util Iterator]
    [damp.ekeko JavaProjectModel EkekoModel WholeProgramAnalysisJavaProjectModel]
    [soot Context MethodOrMethodContext PointsToAnalysis SootMethodRef Local Scene PointsToSet SootClass SootMethod SootField Body Unit ValueBox Value PatchingChain]
    [soot.jimple JimpleBody StaticFieldRef InstanceFieldRef]
    [soot.tagkit SourceLnPosTag LineNumberTag]
    [soot.jimple.internal JAssignStmt JimpleLocal JInstanceFieldRef JIdentityStmt]
    [soot.toolkits.graph ExceptionalUnitGraph]
    [soot.jimple.toolkits.callgraph Edge CallGraph]
    [soot.jimple.toolkits.pointer StrongLocalMustAliasAnalysis LocalMustNotAliasAnalysis LocalMustAliasAnalysis]
    [org.eclipse.jdt.core.dom IBinding VariableBinding ASTNode$NodeList VariableDeclarationFragment FieldDeclaration FieldAccess QualifiedName IVariableBinding SimpleName ITypeBinding Expression ASTNode CompilationUnit]
    ))

;; Soot reification predicates
;; ---------------------------

(defn
  ^WholeProgramAnalysisJavaProjectModel
   current-soot-model
  []
  (.getWholeProgramAnalysisProjectModel ^EkekoModel (ekekomodel/ekeko-model)))

(defn 
  soot-model 
  "Unifies ?model with the current Soot model available to Ekeko,
   or fails when there is currently none."
  [?model]
  (all 
    (!= nil ?model)
    (equals ?model ^WholeProgramAnalysisJavaProjectModel  (current-soot-model))))

(defn 
  soot-model-scene
  "Relation between the current Soot model ?model and its scene.
  
  See also:
  API documentation of soot.Scene
  Unary predicate soot-model/1"
  [?model ?scene]
  (all 
    (soot-model ?model)
    (equals ?scene (.getScene ^WholeProgramAnalysisJavaProjectModel ?model))))

(declare soot-class-method)
(declare soot-class-field)


(defn 
  soot
  "Relation between a SootClass, SootMethod or SootField instance
   ?node and a keyword ?keyword describing its kind. 
   Keyword is one of :class, :method or :field. The instances
   originate from the current Soot model available to Ekeko.

  Examples:
  ;;all SootClass instances
  (ekeko* [?c] (soot :class ?c))
  ;;all SootMethod instances
  (ekeko* [?m] (soot :method ?m))"
  [?keyword ?node]
  (fresh [?model ?scene]
         (soot-model-scene ?model ?scene)
         (conda [(== ?keyword :class) 
                 (conda [(v+ ?node) (all
                                      (succeeds (instance? SootClass ?node))
                                      (succeeds (.isApplicationClass ^SootClass ?node)))]
                        [(v- ?node) (fresh [?classes]
                                           (equals ?classes (.getApplicationClasses ^Scene ?scene))
                                           (contains ?classes ?node))])]
                [(== ?keyword :method) 
                 (conda [(v+ ?node) (fresh [?class]
                                      (succeeds (instance? SootMethod ?node))
                                      (equals ?class (.getDeclaringClass ^ SootMethod ?node))
                                      (!= nil ?class)
                                      (succeeds (.isApplicationClass ^SootClass ?class)))]
                        [(v- ?node) (fresh [?class]
                                           (soot-class-method ?class ?node))])]
                 [(== ?keyword :field) 
                 (fresh [?class]
                        (soot-class-field ?class ?node))]
                )))

;TODO: macro abstracting communalities between next two
(defn 
  soot-class-method
  "Relation between SootClass instances ?class and 
   their SootMethod instances ?method.

  Examples:
  ;;for each class, all methods
  (ekeko* [?c ?m] (soot-class-method ?c ?m))
  "
  [?class ?method]
  (conda [(v+ ?method) (all 
                          (succeeds (instance? SootMethod ?method))
                          (equals ?class (.getDeclaringClass ^SootMethod ?method))
                          (soot :class ?class))]
         [(v- ?method) (fresh [?methods]
                         (soot :class ?class)
                         (equals ?methods (.getMethods ^SootClass ?class))
                         (contains ?methods ?method))]))

(defn
  soot-class-field
  "Relation between SootClass instances ?class 
   and their SootField instances ?field.

  Examples:
  ;;for each class, all fields
  (ekeko* [?c ?f] (soot-class-field ?c ?f))
  "
  [?class ?field]
  (conda [(v+ ?field) (all 
                          (succeeds (instance? SootField ?field))
                          (equals ?class (.getDeclaringClass ^SootField ?field))
                          (soot :class ?class))]
         [(v- ?field) (fresh [?fields]
                         (soot :class ?class)
                         (equals ?fields (.getFields ^SootClass ?class))
                         (contains ?fields ?field))]))

(defn 
  soot-field-signature
  "Relation between SootField instances ?field 
   and their signature string ?signature."
  [?field ?signature]
  (conda [(v+ ?field) (equals-without-exception ?signature (.getSignature ^SootField ?field))]         
         [(v- ?field)
          (conda [(v+ ?signature) 
                  (fresh [?model ?scene]
                         (soot-model-scene ?model ?scene)
                         (equals-without-exception ?field (.getField ^Scene ?scene ?signature)))]
                 [(v- ?signature) 
                  (fresh [?class]
                         (soot-class-field ?class ?field)
                         (soot-field-signature ?field ?signature))])]))

(defn
  soot-class-name
  "Relation between SootClass instances ?class 
   and their fully qualified name string ?name."
  [?class ?name]
  (fresh [?model ?scene]
          (!= nil ?class)
          (!= nil ?name)
          (soot-model-scene ?model ?scene)
          (conda [(v+ ?class) (all (equals ?name (.getName ^SootClass ?class)))]
                 [(v- ?class) 
                  (conda [(v+ ?name) (all (equals-without-exception ?class (.getSootClass ^Scene ?scene ?name)))]
                         [(v- ?name) (all (soot :class ?class) 
                                          (soot-class-name ?class ?name))])])))

(defn 
  soot-method-signature
  "Relation between SootMethod instances ?method 
   and their signature string ?signature."
  [?method ?signature]
  (fresh [?model ?scene]
          (!= nil ?method)
          (!= nil ?signature)
          (soot-model-scene ?model ?scene)
          (conda [(v+ ?method) (all (equals ?signature (.getSignature ^SootMethod ?method)))]
                 [(v- ?method) 
                  (conda [(v+ ?signature) (all (equals-without-exception ?method (.getMethod ^Scene ?scene ?signature)))]
                         [(v- ?signature) (all (soot :method ?method) 
                                          (soot-method-signature ?method ?signature))])])))
(defn 
  soot-method-name
  "Relation between SootMethod instances ?method 
   and their short name string ?name."
  [?method ?name]
  (all
    (soot :method ?method)
    (equals ?name (.getName ^SootMethod ?method))))     

                                                  
(defn 
  soot-entry-method 
  "Unifies ?method with the SootMethod instance that was 
   used as the entry point for Soot's whole-program analyses."
  [?method]
  (fresh [?model ?scene]
         (soot-model-scene ?model ?scene)
         (equals ?method (.getMainMethod ^Scene ?scene))))

(defn 
  soot-class-initializer-method
  "Relation between SootClass instances and 
   their static initializer SootMethod."
  [?class ?method]
  (fresh [?name]
         (soot-class-method ?class ?method) 
         (soot-method-name ?method ?name)
         (equals ?name (SootMethod/staticInitializerName))))

(defn 
  soot-class-constructor-method
  "Relation between SootClass instances 
   and their SootMethod constructors."
  [?class ?method]
  (fresh [?name]
    (soot-class-method ?class ?method) 
    (soot-method-name ?method ?name)
    (equals ?name (SootMethod/constructorName))))


(defn 
  soot-method-body
  "Relation between active SootMethod 
   instances and their JimpleBody.

  Examples: 
  ;;all methods with an active jimple body
  (ekeko* [?m ?b] (soot-method-body ?m ?b))

  See also:
  API reference of soot.jimple.JimpleBody"
  [?method ?body] 
  (conda [(v+ ?body) (all 
                          (succeeds (instance? JimpleBody ?body))
                          (equals ?method (.getMethod ^Body ?body))
                          (soot :method ?method))]
         [(v- ?body) (all
                         (soot :method ?method)
                         (succeeds (.hasActiveBody ^SootMethod ?method))
                         (equals ?body (.getActiveBody ^SootMethod ?method)))]))

(defn-
  qwal-graph-from-soot-cfg 
  [soot-cfg]
  {:soot-cfg soot-cfg
   :nodes (seq (.getUnits ^Body (.getBody ^ExceptionalUnitGraph soot-cfg)))
   :predecessors (fn 
                [node to]
                (all
                  (project [node]
                           (== to (seq (.getPredsOf ^ExceptionalUnitGraph soot-cfg node))))))
   :successors (fn 
                [node to]
                (all
                  (project [node]
                          (== to (seq (.getSuccsOf ^ExceptionalUnitGraph soot-cfg node))))))})


(defn
   soot-method-cfg
   "Relation between a SootMethod ?method and its 
    ExceptionUnitGraph control flow graph, in a 
    format that is suitable for being queried 
    using regular path expressions provided by
    the damp.qwal library.
   
 
    Examples:
    ;;all units on every path through a SOOT control flow graph from an entry point ?entry
    ;;to an exit point ?exit where the exit point uses a value ?defval defined
    ;;by a previous Soot statement ?unit that uses a value ?usedval of type ?keyw
    (ekeko*  [?m ?entry ?exit ?unit ?keyw]
             (fresh [?cfg ?defbox ?exitbox ?usebox ?defval ?usedval]
                    (soot-method-cfg-entry ?m ?cfg ?entry)
                    (soot-method-cfg-exit ?m ?cfg ?exit)
                    (project [?cfg]
                             (qwal ?cfg ?entry ?exit 
                                   []
                                   (q=>*)
                                   (qcurrent [curr] 
                                     (equals curr ?unit) 
                                     (soot-unit-defbox ?unit ?defbox) 
                                     (soot-valuebox-value ?defbox ?defval) 
                                     (soot-unit-usebox ?unit ?usebox)
                                     (soot-valuebox-value ?usebox ?usedval) 
                                     (soot-value ?keyw ?usedval))
                                   (q=>+)
                                   (qcurrent [curr]
                                     (equals curr ?exit) 
                                     (soot-unit-usebox ?exit ?exitbox) 
                                     (soot-valuebox-value ?exitbox ?defval))))))
  See also:
  Documentation of the damp.qwal library.
  Predicates soot-method-cfg-entry and soot-method-cfg-exit which quantify over the 
  Soot heads and tails of the control flow graph.
  API reference of soot.toolkits.graph.ExceptionalUnitGraph"
   [?method ?cfg]
   (conda [(v+ ?cfg)
            (fresh [?body]
                   (equals ?body (.getBody ^ExceptionalUnitGraph (:soot-cfg ?cfg)))
                   (soot-method-body ?method ?body))]
           [(v- ?cfg)
            (fresh [?body]
                   (soot-method-body ?method ?body)
                   (equals ?cfg (qwal-graph-from-soot-cfg (ExceptionalUnitGraph. ^Body ?body))))]))


(defn 
  soot-method-cfg-entry
   "Relation between a Soot ExceptionalUnitGraph 
    (in a format compatible with damp.qwal) and its entry points.
   
    See also:
    Binary predicate soot-method-cfg/2
    API reference of soot.toolkits.graph.ExceptionalUnitGraph"
  [?method ?cfg ?entry]
  (fresh [?entries]
         (soot-method-cfg ?method ?cfg)
         (equals ?entries (.getHeads ^ExceptionalUnitGraph (:soot-cfg ?cfg)))
         (contains ?entries ?entry)))
  
(defn 
  soot-method-cfg-exit
   "Relational between a Soot ExceptionalUnitGraph
    (in a format compatible with damp.qwal) and its exit points.
   
    See also:
    Binary predicate soot-method-cfg/2
    API reference of soot.toolkits.graph.ExceptionalUnitGraph"
  [?method ?cfg ?exit]
  (fresh [?exits]
         (soot-method-cfg ?method ?cfg)
         (equals ?exits (.getTails ^ExceptionalUnitGraph (:soot-cfg ?cfg)))
         (contains ?exits ?exit)))


;; SOOT Units and Values
;; ---------------------


(defn 
  soot-method-units 
  "Relation between a SootMethod ?m and the PatchingChain of 
   units ?units in its active JimpleBody."
  [?m ?units]
  (fresh [?body] 
    (soot-method-body ?m ?body)
    (equals ?units (.getUnits ^Body ?body))))

(defn 
  soot-method-unit-trailing-unit
  "Relation between a SootMethod ?m and two of its units 
   such that unit ?trailing comes after ?unit in 
   the PatchingChain of its JimpleBody."
  [?m ?unit ?trailing]
  (fresh [?units ?trailingUnits]
         (soot-method-units ?m ?units)
         (equals ?trailingUnits (iterator-seq (.iterator ^PatchingChain ?units ?unit)))
         (contains ?trailingUnits ?trailing)))
         
(defn 
  soot-unit 
  "Relation between soot unit ?u and a keyword ?keyw describing its kind.
   The instances originate from the current Soot model available to Ekeko.
   
   See also: 
   API documentation of soot.Unit"
  [?keyw ?u]
  (all
    (conda
      [(v+ ?u)
       (succeeds (instance? Unit ?u))]
      [(v- ?u)
       (fresh [?m ?units]
              (soot-method-units ?m ?units)
              (contains ?units ?u))])
    (equals ?keyw (astnode/ekeko-keyword-for-class (class ?u)))))

(defn
  soot-unit-defbox
  "Relation between a soot Unit ?u and one of the ValueBox instances it defines.

  See also:
  predicate soot-unit-userbox/2"
  [?u ?b]
  (fresh [?boxes ?keyw]
         (soot-unit ?keyw ?u)
         (equals ?boxes (.getDefBoxes ^Unit ?u))
         (contains ?boxes ?b)))

(defn 
  soot-unit-usebox
  "Relation between a soot Unit ?u and one of the ValueBox instances it uses.

  See also:
  predicate soot-unit-userbox/2"
  [?u ?b]
  (fresh [?boxes ?keyw]
         (soot-unit ?keyw ?u)
         (equals ?boxes (.getUseBoxes ^Unit ?u))
         (contains ?boxes ?b)))

(defn 
  soot-valuebox 
  "Non-relational. Verifies that ?b is a soot ValueBox."
  [?b]
  (all
    (succeeds (instance? ValueBox ?b))))

(defn 
  soot-valuebox-value
  "Non-relational. Unifies ?v with the Value inside the ValueBox ?b."
  [?b ?v]
  (all
    (soot-valuebox ?b)
    (equals ?v (.getValue ^ValueBox ?b))))

(defn 
  soot-value
  "Non-relational. Unifies ?keyw with the keyword 
   that represents the kind of the soot Value ?v."
  [?keyw ?v]
  (all
    (succeeds (instance? Value ?v))
    (equals ?keyw (astnode/ekeko-keyword-for-class (class ?v)))))


; Assignment units

(defn 
  soot-unit-assign-leftop
  "Relation between the soot JAssignStmt unit ?u 
   and its left operand ?local."
  [?u ?local]
  (all
     (soot-unit :JAssignStmt ?u)
     (equals ?local (.getLeftOp ^JAssignStmt ?u))))

(defn 
  soot-unit-assign-rightop
   "Relation between the soot JAssignStmt unit ?u 
   and its right operand ?local."
  [?u ?local]
  (all
     (soot-unit :JAssignStmt ?u)
     (equals ?local (.getRightOp  ^JAssignStmt ?u))))



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
           (soot-model-scene ?model ?scene)
           (ast-model :TypeDeclaration ?jdt ?model) 
           (type-declaration-signature ?jdt ?binaryname)
           (soot-class-name ?soot ?binaryname)))


(def 
 #^{:doc 
    "Relation between the JDT MethodDeclaration ?jdt
     and the corresponding SootMethod ?soot.
     Tabled."}
  ast-soot-method 
  (tabled
    [?jdt ?soot]
    (fresh [?model ?scene ?signature]
           (soot-model-scene ?model ?scene)
           (ast-model :MethodDeclaration ?jdt ?model) 
           (method-declaration-signature ?jdt ?signature)
           (soot-method-signature ?soot ?signature))))

(defn 
  ast-soot-field 
  "Relation between the JDT FieldDeclaration ?declaration,
   one of its VariableDeclarationFragment instances ?field,
   and the corresponding SootField ?soot.

  Note that a field declaration AST node can declare multiple fields."
  [?declaration ?field ?soot]
    (fresh [?model ?scene ?signature]
           (soot-model-scene ?model ?scene)
           (ast-model :FieldDeclaration ?declaration ?model) 
           (child :fragments ?declaration ?field)
           (field-signature ?field ?signature)
           (soot-field-signature ?soot ?signature)))

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
    (soot-unit ?unitkeyw ?unit)))

;todo: arrays
(defn 
  ast-reference-soot-model
  "Relation between reference-valued JDT Expression instances ?ast
   and the Ekeko model ?model in which they reside."
  [?ast ?model]
  (fresh [?scene ?keyw ?binding]
         (soot-model-scene ?model ?scene)
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
                           (soot-class-initializer-method ?sc ?sm)]
                          [(soot-class-constructor-method ?sc ?sm)]))])))

;TODO: assumes SOOT has been run with variable name preservation option on (was not the case for SOUL's Cava)
(defn 
  is-ast-soot-name-local-compatible?
  "Non-relational. Verifies that JDT SimpleName instance ?name
   has the same identifier as Soot JimpleLocal ?local."
  [?name ?local]
  (fresh [?identifier]
         (ast :SimpleName ?name)
         (soot-value :JimpleLocal ?local)
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
    (soot-value :JimpleLocal ?local)
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
         (soot-field-signature ?sf ?sig)
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
                                [(soot-value :JimpleLocal ?base)]))])))


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
         (soot-field-signature ?sf ?sig)
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
                                [(soot-value :JimpleLocal ?base)]))])))



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
    (soot-unit-assign-leftop ?unit ?left)
    (soot-value :JimpleLocal ?left)
    (soot-unit-assign-rightop ?unit ?right)
    (soot-value ?valkeyw ?right)    
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
    (soot-unit-assign-rightop ?unit ?right)
    (soot-value :JimpleLocal ?right)
    (soot-unit-assign-leftop ?unit ?left)    
    (soot-value ?lvalkeyw ?left)    
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
           (soot-method-units ?m ?units)
           (contains ?units ?unit)
           (is-ast-soot-unit-position-compatible? ?expression ?unit)
           ;this conde cannot be changed into a conda without getting errors (TODO: check invariants that govern use of conda)
           (conde [;identity or assigment unit assigning to an expression that is a local name
                   (conda [(ast :SimpleName ?expression)]
                          [(ast :ThisExpression ?expression)])
                   (fresh [?unitKeyw]
                          (soot-unit ?unitKeyw ?unit)
                          ;silly conda to enable type hinting
                          (conda [(== ?unitKeyw :JIdentityStmt) (equals ?local (.getLeftOp ^JIdentityStmt ?unit))]
                                 [(== ?unitKeyw :JAssignStmt) (equals ?local (.getLeftOp ^JAssignStmt ?unit))])
                          (is-ast-unit-identity-or-assignment-compatible-local-on-lhs? ?expression ?unitKeyw ?unit))]
                  [ ;assignment unit where expression is on the RHS, local on the LHS
                   (is-ast-unit-assign-compatible-local-on-lhs? ?expression ?unit)
                   (soot-unit-assign-leftop ?unit ?local)
                   ;TODO: Cava used to check wjether a subsequent unit uses the local as the expression's parent
                   ;(fresh [?trailingUnit]
                   ;       (soot-method-unit-trailing-unit ?m ?unit ?trailingUnit))
                   ]
                  [
                   ;assignment unit where expression on the LHS, local is on the RHS
                   (is-ast-unit-assign-compatible-local-on-rhs? ?expression ?unit)
                   (soot-unit-assign-rightop ?unit ?local)]
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



;; Soot Call Graph
;; ---------------

;(defn soot-call-graph [?g]
;  (fresh [?model ?scene]
;    (soot-model-scene ?model ?scene)
;    (equals ?g (.getCallGraph ^Scene ?scene))
;    (!= ?g nil)))


;TODO: see comment in Java model, otherwise could have implemented it in core.logic rather than in Java
;something is wrong with the implementation of equals(Object) on Edge
(defn 
  soot-method-calls-method
  "Relation between SootMethod ?m and one of the SootMethod instances ?callee
   that are called from within ?m.
  
   The relation is computed based on the points-to set of the receiver (i.e., based
   on an approximation of the dynamic rather than the static type of the receiver). 

   Note that library methods are not included. 

   See also: 
   Predicate soot-method-called-by-method/2 is declaratively equivalent, 
   but operationally more efficient when the callee is known and the callers aren't."
  [?m ?callee] 
  (fresh [?model ?scene ?methods]
         (soot-model-scene ?model ?scene)
         (soot :method ?m)
         (equals ?methods (iterator-seq (.dynamicMethodCallees ^WholeProgramAnalysisJavaProjectModel ?model ?m)))
         (contains ?methods ?callee)
         (soot :method ?callee) ;application methods only
         ))

;declaratively, same as above
(defn 
  soot-method-called-by-method
   "Relation between SootMethod ?m and one of the SootMethod instances ?caller
   that invoke ?m.
  
   The relation is computed based on the points-to set of the receiver (i.e., based
   on an approximation of the dynamic rather than the static type of the receiver). 

   Note that library methods are not included. 

   See also: 
   Predicate soot-method-calls-method/2 is declaratively equivalent, 
   but operationally more efficient when the caller is known and the callees aren't."
  [?m ?caller] 
  (fresh [?model ?scene ?methods]
         (soot-model-scene ?model ?scene)
         (soot :method ?m)
         (equals ?methods (iterator-seq (.dynamicMethodCallers ^WholeProgramAnalysisJavaProjectModel ?model ?m)))
         (contains ?methods ?caller)
         (soot :method ?caller) ;application methods only
         ))

(defn 
  soot-method-called-by-unit 
  "Relation between SootMethod ?m and one of the Unit instances ?unit 
   it is invoked by.

   The relation is computed based on the points-to set of the receiver (i.e., based
   on an approximation of the dynamic rather than the static type of the receiver). 

   Note that library methods are not included. 

   See also: 
   Predicate soot-unit-calls-method/2 is declaratively equivalent, 
   but operationally more efficient when the unit is known and the method isn't.
  "
  [?m ?unit] 
  (fresh [?model ?keyw ?scene ?units]
         (soot-model-scene ?model ?scene)
         (soot :method ?m)
         (equals ?units (iterator-seq (.dynamicUnitCallers ^WholeProgramAnalysisJavaProjectModel ?model ?m)))
         (contains ?units ?unit)
         (soot-unit ?keyw  ?unit) 
         ))

;declaratively, same as above
(defn 
  soot-unit-calls-method
  "Relation between Unit ?unit and one of the SootMethod instances ?m
   it may invoke. 

   The relation is computed based on the points-to set of the receiver (i.e., based
   on an approximation of the dynamic rather than the static type of the receiver). 

   Note that library methods are not included. 

   See also: 
   Predicate soot-method-called-by-unit/2 is declaratively equivalent, 
   but operationally more efficient when the method is known and the unit isn't.
  "
  [?unit ?m] 
  (fresh [?model ?keyw ?scene ?methods]
         (soot-model-scene ?model ?scene)
         (soot-unit ?keyw ?unit)
         (equals ?methods (iterator-seq (.dynamicUnitCallees ^WholeProgramAnalysisJavaProjectModel ?model ?unit)))
         (contains ?methods ?m)
         (soot :method ?m) ;application methods only
         ))


  
  ;; ALIASING
;; --------

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
           (soot-model-scene ?model ?scene)
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




(defn 
  soot-method-local-must-alias-analysis
  "Relation between a SootMethod ?m and 
  its intra-procedural must alias analysis ?a."
  [?m ?a]
  (fresh [?cfg ?unitgraph]
         (soot-method-cfg ?m ?cfg)
         (equals ?unitgraph (:soot-cfg ?cfg))
         (equals ?a (LocalMustAliasAnalysis. ?unitgraph))))

(defn 
  soot-method-local-must-not-alias-analysis 
  "Relation between a SootMethod ?m and 
  its intra-procedural must-not alias analysis ?a."
  [?m ?a]
  (fresh [?cfg ?unitgraph]
         (soot-method-cfg ?m ?cfg)
         (equals ?unitgraph (:soot-cfg ?cfg))
         (equals ?a (LocalMustNotAliasAnalysis. ?unitgraph))))

(defn 
  soot-method-local-must-alias-analysis-strong
  "Relation between a SootMethod ?m and 
  its intra-procedural strong must alias analysis ?a."
  [?m ?a]
  (fresh [?cfg ?unitgraph]
         (soot-method-cfg ?m ?cfg)
         (equals ?unitgraph (:soot-cfg ?cfg))
         (equals ?a (StrongLocalMustAliasAnalysis. ?unitgraph))))

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
         (soot-method-local-must-alias-analysis ?m ?a)
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
         (soot-method-local-must-alias-analysis-strong ?m ?a)
         (ast-reference-model-soot-unit-local ?ast1 ?model ?unit1 ?local1)
         (ast-reference-model-soot-unit-local ?ast2 ?model ?unit2 ?local2) 
         (succeeds (.mustAlias  ^StrongLocalMustAliasAnalysis ?a ?local1 ?unit1 ?local2 ?unit2))))


              