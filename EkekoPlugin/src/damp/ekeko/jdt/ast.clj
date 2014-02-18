(ns 
  ^{:doc "Low-level relations of JDT AST nodes."
    :author "Coen De Roover"}
  damp.ekeko.jdt.ast
  (:refer-clojure :exclude [== type])
  (:require [clojure.core [logic :as l]])
  (:require 
    [damp.ekeko
     [logic :as el] [ekekomodel :as ekekomodel]]
    [damp.ekeko.jdt 
     [javaprojectmodel :as javaprojectmodel] [astnode :as astnode] [bindings :as bindings]])
  (:import 
    [java.util Map]
    [damp.ekeko JavaProjectModel]
    [damp.ekeko EkekoModel]
    [dk.itu.smartemf.ofbiz.analysis ControlFlowGraph]
    [org.eclipse.core.runtime IProgressMonitor]
    [org.eclipse.jdt.core IJavaElement ITypeHierarchy IType IPackageFragment IClassFile ICompilationUnit
     IJavaProject WorkingCopyOwner IMethod]
    [org.eclipse.jdt.core.dom Expression IVariableBinding ASTParser AST IBinding Type TypeDeclaration 
     QualifiedName SimpleName ITypeBinding MethodDeclaration
     MethodInvocation ClassInstanceCreation SuperConstructorInvocation SuperMethodInvocation
     SuperFieldAccess FieldAccess ConstructorInvocation ASTNode ASTNode$NodeList CompilationUnit
     Annotation IAnnotationBinding TypeLiteral]))
     
(set! *warn-on-reflection* true)

(declare nodes-of-type)

;TODO: implement variant of defne that takes modes for variables 

;; AST Nodes
;; ---------

(defn 
  ast 
  "Reifies the relation between ASTNode instances ?node
   and their kind ?keyword.
   In general, ?keyword is the keyword that corresponds 
   to the capitalized, unqualified name of ?node's class.
  
   Examples:
   ;; all method declarations
   (ekeko* [?node] (ast :MethodDeclaration ?node))
   ;; all ast nodes of any kind
   (ekeko* [?keyword ?node] (ast ?keyword ?node))
   
   See also:
   API documentation of org.eclipse.jdt.core.dom.ASTNode"
  [?keyword ?node]
  (l/conda [(el/v+ ?node) (l/conda [(el/v+ ?keyword) (l/all (el/succeeds-without-exception (instance? (astnode/class-for-ekeko-keyword ?keyword) ?node)))]
                            [(el/v- ?keyword) (l/all (el/succeeds (astnode/ast? ?node))
                                                (el/equals ?keyword (astnode/ekeko-keyword-for-class-of ?node)))])]
         [(el/v- ?node) (l/conda [(el/v+ ?keyword) (l/fresh [?nodes]
                                                  (el/equals ?nodes (nodes-of-type ?keyword))
                                                  (el/contains ?nodes ?node))]
                            [(el/v- ?keyword) (l/fresh [?keywords]
                                                  (el/contains astnode/ekeko-keywords-for-ast-classes ?keyword)
                                                  (ast ?keyword ?node))])]))

;TODO: reify actual property descriptors 
(defn
  has
  "Reifies the relation between an ASTNode instance ?node 
  and the value ?child of its property named ?keyword.
   
   Here, ?keyword is the keyword that corresponds to the
   decapitalized name of the property's PropertyDescriptor. 
   In general, ?child is either another ASTNode instance, 
   a primitive value or an instance of ASTNode$NodeList. 

   Examples:
   ;; method invocations and their receiver
   (ekeko* [?inv ?exp]
     (ast :MethodInvocation ?inv) 
     (has :expression ?inv exp)) 
   ;; all properties of all compilation units 
   (ekeko* [?cu ?key ?child] 
     (ast :CompilationUnit ?cu) 
     (has ?key ?cu ?child))
   
   See also: 
   Ternary predicate child/3 
   API documentation of org.eclipse.jdt.core.dom.ASTNode 
   and org.eclipse.jdt.core.dom.StructuralPropertyDescriptor"
  [?keyword ?node ?child]
   (l/conda [(el/v+ ?node) (l/conda [(el/v+ ?keyword) 
                              (l/fresh [?childretrievingf]
                                    (el/equals ?childretrievingf (?keyword (astnode/reifiers ?node)))
                                    (l/!= ?childretrievingf nil)
                                    (el/equals ?child (?childretrievingf)))]
                             [(el/v- ?keyword) (l/fresh [?keywords]
                                                  (el/equals ?keywords (keys (astnode/reifiers ?node)))
                                                  (el/contains ?keywords ?keyword)
                                                  (has ?keyword ?node ?child))])]
          [(el/v- ?node) (l/conda [(el/v+ ?child) (l/all (el/equals ?node (astnode/owner ?child)) 
                                               (has ?keyword ?node ?child))]
                             [(el/v- ?child) (l/fresh [?astkeyw]
                                              (ast ?astkeyw ?node)
                                              (has ?keyword ?node ?child))])]))


(defn 
  child 
   "Reifies the relation between an ASTNode instance ?node 
    and its ASTNode children ?child that are either the value
    of the child property or an element of the child list 
    property named ?keyword. 
    In contrast to has/3, ?child is therefore always an
    instance of ASTNode. 

    Note that ?keyword is the keyword that corresponds to 
    the decapitalized name of the property's PropertyDescriptor.
   
    Examples:
    ;;?child is any of the arguments to the method invocation ?inv
    (ekeko* [?inv ?child] (ast :MethodInvocation ?inv)
                          (child :arguments ?inv ?child))

    See also: 
    Ternary predicate has/3 
    API documentation of org.eclipse.jdt.core.dom.ASTNode 
    and org.eclipse.jdt.core.dom.StructuralPropertyDescriptor"
  [?keyword ?node ?child]
  (l/fresh [?ch] 
    (has ?keyword ?node ?ch)
    (l/conda
           [(el/succeeds (astnode/ast? ?ch))
            (l/== ?child ?ch)] 
           [(el/succeeds (astnode/lstvalue? ?ch))
            (el/contains (:value ?ch) ?child)])))
            
(defn 
  child+
  "Transitive closure of the child relation.
   In other words, ?child is an ASTNode instance that resides at
   an arbitrary depth within the ASTNode instance ?node. 

  Operationally, a recursive descent is performed through ?node. 

  Examples:
  ;;?exp is an expression within method declaration ?m
  (ekeko* [?m ?ch] (ast :MethodDeclaration ?m)
                   (child+ ?m ?ch))   

  See also:
  Ternary predicate child/3"
  [?node ?child]
  (l/fresh [?keyw ?ch]
    (child ?keyw ?node ?ch)
    (l/conde [(l/== ?ch ?child)]
           [(child+ ?ch ?child)])))

;tabled version is much slower (169294ms vs 5990ms on jHotDraw)
;but might be faster if multiple child+ conditions are used in a query

;(def tabled-child+ 
;  (tabled [?node ?child]
;   (l/fresh [?keyw ?ch]
;          (child ?keyw ?node ?ch)
;          (l/conde [(l/== ?ch ?child)]
;                [(tabled-child+ ?ch ?child)]))))    
  

(defn
  value
  "Relation of ASTNode property values that aren't ASTNode themselves:
   nil, primitive values and lists."
  [?val]
  (l/conda [(el/v+ ?val)
          (el/succeeds (astnode/value? ?val))]
         [(el/v- ?val)
          (l/fresh [?kind ?ast ?property]
                 (ast ?kind ?ast)
                 (has ?property ?ast ?val)
                 (value ?val))]))
  


(defn
  value|null
  "Relation of all null-valued ASTNode property values."
  [?val]
  (l/all
    (value ?val)
    (el/succeeds (astnode/nilvalue? ?val))))

(defn
  value|list
  "Relation of all list-valued ASTNode property values."
  [?val]
  (l/all
    (value ?val)
    (el/succeeds (astnode/lstvalue? ?val))))

(defn
  value|primitive 
  "Relation of all primitive-valued ASTNode property values."
  [?val]
  (l/all
    (value ?val)
    (el/succeeds (astnode/primitivevalue? ?val))))

(defn
  value-raw
  "Relation of an ASTNode property value that isn't an ASTNode itself
   and its raw value (either a primitive, nil, or a list)."
  [?val ?raw]
  (l/all
    (value ?val)
    (el/equals ?raw (:value ?val))))
          
(defn
  ast-parent
  "Relation between an ASTNode instance ?ast and its parent node 
   ?parent. Note that this predicate fails for CompilationUnit instances, 
   which function as the root of the AST.
   
   See also:
   API documentation of org.eclipse.jdt.core.dom.ASTNode" 
  [?ast ?parent]
  (l/fresh [?key]
    (ast ?key ?ast)
    (el/equals ?parent (.getParent ^ASTNode ?ast))
    (l/!= nil ?parent)))

(defn 
  ast-root
  "Relation between an ASTNode instance ?ast and the root of its tree ?cu,
   an instance of CompilationUnit.

   See also:
   API documentation of org.eclipse.jdt.core.dom.ASTNode"
  [?ast ?cu]
  (l/fresh [?key]
    (ast ?key ?ast)
    (el/equals ?cu (.getRoot ^ASTNode ?ast))))

(defn 
  ast|resolveable 
  "Like binary ast/2 predicate, but ensures ?ast is an ASTNode
   instance that can be resolved to an IBinding 
   (i.e., implements method resolveBinding).

   See also:
   Binary predicate ast/2
   API documentation of org.eclipse.jdt.core.dom.IBinding"
  [?key ?ast]
  (let  [keys astnode/ekeko-keywords-for-resolveable-ast-classes]
    (l/all
         (el/contains keys ?key)
         (ast ?key ?ast))))

(defn 
  ast|declaration 
   "Like binary ast/2 predicate, but ensures ?ast is
    a declaration ASTNode instance.

   See also:
   Binary predicate ast/2"
  [?key ?ast]
  (let [declaration-keywords astnode/ekeko-keywords-for-declaration-ast-classes]
    (l/all 
      (el/contains declaration-keywords ?key)
      (ast ?key ?ast))))

;TODO: remove pseudo-node keywords 
(defn 
  ast|fieldaccess
   "Like binary ast/2 predicate, but ensures ?ast is 
    an ASTNode that accesses a field.

    Note that ?ast can be an instance of FieldAccess, 
    SuperFieldAccess, SimpleName or QualifiedName."
  [?key ?node] 
  (l/conde [(el/v- ?node)
          (ast :FieldAccessLike ?node) 
          (ast ?key ?node)]
         [(el/v+ ?node) 
          (l/fresh [?n] 
                 (ast :FieldAccessLike ?n)
                 (l/== ?n ?node))]))
    
(defn 
  ast|type
  "Like ast/2, but ensures ?type is an 
   instance of one of the Type subclasses.
   Note that these are references to types within the source code, 
   rather than a canonical representation of a type."
  [?key ?type] 
  (l/all 
    (ast :Type ?type) ;above trick is not necessary as Type is an actual super class 
    (ast ?key ?type)))

(defn 
  ast|declaration|resolveable
   "Like binary ast/2 predicate, but ensures ?ast 
    is a resolveable declaration ASTNode instance.

   See also:
   Binary predicates ast|resolveable/2 and ast|declaration/2."
  [?key ?ast]
  (l/all 
    (ast|declaration ?key ?ast)
    (ast|resolveable ?key ?ast)))

(defn 
  ast|invocation 
  "Like binary ast/2, but ensures ?ast is an ASTNode 
   that invokes a method or constructor.

   Note that ?ast can be an instance of MethodInvocation, 
   SuperMethodInvocation, ClassInstanceCreation,
   ConstructorInvocation or SuperConstructorInvocation"
  [?key ?node] 
  (l/conde [(el/v- ?node)
          (ast :MethodInvocationLike ?node)
          (ast ?key ?node)]
         [(el/v+ ?node)
          (ast ?key ?node)
          (el/contains [:MethodInvocation 
                        :SuperMethodInvocation
                        :ClassInstanceCreation 
                        :ConstructorInvocation 
                        :SuperConstructorInvocation] ?key) ;TODO: define a constant for this
          ]))
 
(declare ast|expression-binding|type)

(defn 
  ast|expression|reference
  "Relation between a reference-valued Expression instance ?ast and 
   the keyword ?keyw representing its kind."
  [?keyw ?ast]
  (l/fresh [?binding]
         (ast|expression-binding|type ?keyw ?ast ?binding)
         (el/succeeds (not (.isPrimitive ^ITypeBinding ?binding))))) 

(defn 
  ast|parameter|name
  "Like ast/2, but ensures ?name is the SimpleName 
   of a formal parameter declaration."
  [?keyw ?name]
  (l/fresh [?nameBinding ?parent]
         (l/== ?keyw :SimpleName)
         (ast ?keyw ?name) 
         (el/equals ?nameBinding (.resolveBinding ^SimpleName ?name))
         (el/succeeds (.isParameter ^IVariableBinding ?nameBinding))
         (ast-parent ?name ?parent)
         (ast :SingleVariableDeclaration ?parent)))


(defn 
  ast-location 
  "Relation between ASTNode ?ast and Clojure vector ?locationVector 
  representing its location: [begin-character-index-in-file, 
                              end-chararacter-index-in-file, 
                              begin-line-number, 
                              end-line-number]
  "
  [?ast ?locationVector]
  (l/fresh [?root]
         (ast-root ?ast ?root)
         (el/equals ?locationVector
                 (let [astStart (.getStartPosition ^ASTNode ?ast)
                       astEnd (+ astStart (.getLength ^ASTNode ?ast))
                       astLineStart (.getLineNumber ^CompilationUnit ?root astStart)
                       astLineEnd (.getLineNumber ^CompilationUnit ?root astEnd)]
                   [astStart astEnd astLineStart astLineEnd]))))

(defn 
  ast-typedeclaration|encompassing
  "Relation between ASTNode ?ast and the TypeDeclaration ?t that encompasses it."
  ([?ast ?t] 
    (l/fresh [?keyw ?parent]
             (ast ?keyw ?ast)
             (ast-parent ?ast ?parent)
             (ast-typedeclaration|encompassing ?ast ?t ?parent)))           
  ([?ast ?t ?ancestor]
    (l/conde [(ast :TypeDeclaration ?ancestor) 
              (l/== ?ancestor ?t)]
             [(l/fresh [?parent]
                     (ast-parent ?ancestor ?parent)
                     (ast-typedeclaration|encompassing ?ast ?t ?parent))])))

;TODO: implement a parent+ analogous to child+
(defn 
  ast-methoddeclaration|encompassing|nonfailing
  "Relation between ASTNode ?ast and either the MethodDeclaration ?m that encompasses it, 
   or nil if there is no encopassing MethodDeclaration.

   Operationally, performs a recursive ascend.
 
   See also: 
   Predicate ast-encompassing-method/2 which fails if there is no encompassing method."
  ([?ast ?m-or-nil] 
    (l/fresh [?keyw ?parent]
             (ast ?keyw ?ast)
             (ast-parent ?ast ?parent)
             (ast-methoddeclaration|encompassing|nonfailing ?ast ?m-or-nil ?parent)))           
  ([?ast ?m ?ancestor]
    (l/conde
      [(ast :TypeDeclaration ?ancestor) 
       (l/== nil ?m)]
      [(ast :MethodDeclaration ?ancestor) 
       (l/== ?ancestor ?m)]
      [(l/fresh [?parent]
              (ast-parent ?ancestor ?parent)
              (ast-methoddeclaration|encompassing|nonfailing ?ast ?m ?parent))])))

(defn 
  ast-methoddeclaration|encompassing
  "Relation between ASTNode ?ast and the MethodDeclaration ?m that encompasses it.

   Operationally, performs a recursive ascend.
 
   See also: 
   Predicate ast-encompassing-method-non-failing/2 unifies ?m with nil
   if there is no encompassing method."
  [?ast ?m]
  (l/all
    (l/!= nil ?m)
    (ast-methoddeclaration|encompassing|nonfailing ?ast ?m)))

(defn 
  ast|declaration-modifier
  "Reifies the relation between a declaration AST node ?ast 
   of kind ?key and any of its modifiers ?mod.

   See also: 
   Ternary predicate ast/3"
  [?key ?ast ?mod]
  (l/all
    (el/contains [:AnnotationTypeMemberDeclaration :FieldDeclaration :MethodDeclaration :SingleVariableDeclaration :TypeDeclaration :EnumDeclaration :EnumConstantDeclaration :AnnotationTypeDeclaration] ?key)
    (ast|declaration ?key ?ast)
    (child :modifiers ?ast ?mod)))

;; Control Flow Graph
;; ------------------


(defn 
  method-statements
  "Relation between a MethodDeclaration ?m and its
   ASTNode$NodeList list of statements ?slist in its body. 
   Note that abstract methods are not included in this relation, 
   but methods with en empty body are."
  [?m ?statements]
  (l/fresh [?body ?slist]
         (ast :MethodDeclaration ?m)
         (has :body ?m ?body)
         (ast :Block ?body)
         (has :statements ?body ?slist)
         (value-raw ?slist ?statements)))

(defn- 
  jdt-method-cfg [m]
  (let [cu (.getRoot ^MethodDeclaration m)]
    (when-let [el (.getJavaElement ^CompilationUnit cu)]
      (when-let [ijp (.getJavaProject el)]
        (when-let [ejpm (.getJavaProjectModel ^EkekoModel (damp.ekeko.ekekomodel/ekeko-model) ijp)]
          (.getControlFlowGraph ejpm m))))))

(defn-
  qwal-graph-from-jdt-cfg [jdt-cfg]
  {:nodes (seq (.keySet (.successors ^ControlFlowGraph jdt-cfg)))
   :predecessors (fn 
                   [node to]
                   (l/all
                     (l/project [node]
                              (l/== to (seq (.get ^Map (.predecessors ^ControlFlowGraph jdt-cfg) node))))))
   :successors (fn 
                [node to]
                (l/all
                  (l/project [node]
                           (l/== to (seq (.get ^Map (.successors ^ControlFlowGraph jdt-cfg) node))))))})
 
(defn 
  method-cfg
  "Relation between a MethodDeclaration ?m and its control
   flow graph ?cfg, in a format suitable to be queried with
   the regular path expressions provided by the damp.qwal libraries.
 
  Examples:
  ;;all methods of which all complete paths through their 
  ;;control flow graph end in a ReturnStatement ?return that 
  ;;is immediately preceded by exactly one other statement ?beforeReturn 
   (ekeko* [?m ?cfg ?entry ?end]
           (method-cfg ?m ?cfg) 
           (method-cfg-entry ?m ?entry)
           (l/fresh [?beforeReturn ?return]
                  (l/project [?cfg]
                           (qwal ?cfg ?entry ?end 
                                 []
                                 (qcurrent [currentStatement] (el/equals currentStatement ?beforeReturn))
                                 q=>
                                 (qcurrent [currentStatement] (el/equals currentStatement ?return) (ast :ReturnStatement ?return))))))

  See also:
  Documentation of the damp.qwal library.
  Predicates method-cfg-entry and method-cfg-exit which quantify 
  over the symbolic entry and exit point of a method's control flow graph."
  [?m ?cfg]
  (l/fresh [?g] 
    (ast :MethodDeclaration ?m)
    (el/equals ?g (jdt-method-cfg ?m))
    (l/!= ?g nil)
    (el/equals ?cfg (qwal-graph-from-jdt-cfg ?g))))

(defn-
  method-statement|first
  [?m ?s]
  (l/fresh [?slist]
       (method-statements ?m ?slist)
       (el/succeeds (> (.size ^ASTNode$NodeList ?slist) 0)) 
       (el/equals ?s (first ?slist))))       

(defn 
  method-cfg|entry
  "Relation between MethodDeclaration ?m and the statement ?entry 
   that represents the entry point of its control flow graph."
  [?m ?entry]
  (l/all 
    (method-statement|first ?m ?entry)))

(defn 
  method-cfg|exit 
  "Relation between MethodDeclaration ?m and the Block ?exit that
   represents the symbolic exit point of its control flow graph.

  See also:
  Predicate method-cfg/2 and documentation of the damp.qwal library."
  [?m ?exit]
  (l/all 
   (ast :MethodDeclaration ?m)
   (has :body ?m ?exit)
   (ast :Block ?exit)))


; Node generators called by reification goals
; -------------------------------------------

(defn- subclass-nodes-of-type [t]
  (let [c (astnode/class-for-ekeko-keyword t)
        s (supers c)]
    (cond (and (not (= c org.eclipse.jdt.core.dom.SimpleName))
               (contains? s org.eclipse.jdt.core.dom.Expression))
            (filter (fn [n] (instance? c n))
              (nodes-of-type :Expression))
          (contains? s org.eclipse.jdt.core.dom.Statement)
            (filter (fn [n] (instance? c n))
              (nodes-of-type :Statement))
          (contains? s org.eclipse.jdt.core.dom.Type)
            (filter (fn [n] (instance? c n))
              (nodes-of-type :Type))
          :else 
            (l/run-nc* [?n] 
              (l/fresh [?cu]
                (ast :CompilationUnit ?cu)
                (child+ ?cu ?n)
                (el/succeeds (instance? c ?n)))))))

(defn- logic-all-import-declarations []
  (l/run-nc* [?importDeclaration]
        (l/fresh [?cu]
               (ast :CompilationUnit ?cu)
               (child :imports ?cu ?importDeclaration))))

(defn- logic-all-package-declarations []
  (l/run-nc* [?packageDeclaration]
        (l/fresh [?cu]
               (ast :CompilationUnit ?cu)
               (child :package ?cu ?packageDeclaration))))


(defn- nodes-of-type [t]
  (let [models (javaprojectmodel/java-project-models)]
    (case t
      :CompilationUnit (mapcat (fn [java-project-model] (.getCompilationUnits ^JavaProjectModel java-project-model)) models)
      :MethodDeclaration (mapcat (fn [java-project-model]  (.getMethodDeclarations ^JavaProjectModel java-project-model)) models)
      :TypeDeclaration (mapcat (fn [java-project-model]  (.getTypeDeclarations ^JavaProjectModel java-project-model)) models)
      :AnnotationTypeDeclaration (mapcat (fn [java-project-model]  (.getAnnotationTypeDeclarations ^JavaProjectModel java-project-model)) models)
      :EnumDeclaration (mapcat (fn [java-project-model]  (.getEnumDeclarations ^JavaProjectModel java-project-model)) models)
      :AnonymousClassDeclaration (mapcat (fn [java-project-model]  (.getAnonymousClassDeclarations ^JavaProjectModel java-project-model)) models)
      :Statement (mapcat (fn [java-project-model]  (.getStatements ^JavaProjectModel java-project-model)) models)
      :Expression (mapcat (fn [java-project-model]  (.getExpressions ^JavaProjectModel java-project-model)) models)
      :ImportDeclaration (logic-all-import-declarations)
      :PackageDeclaration (logic-all-package-declarations)
      :FieldDeclaration (mapcat (fn [java-project-model]  (.getFieldDeclarations ^JavaProjectModel java-project-model)) models)
      :SingleVariableDeclaration (mapcat (fn [java-project-model]  (.getSingleVariableDeclarations ^JavaProjectModel java-project-model)) models)
      :EnumConstantDeclaration (mapcat (fn [java-project-model]  (.getEnumConstantDeclarations ^JavaProjectModel java-project-model)) models)
      :AnnotationTypeMemberDeclaration (mapcat (fn [java-project-model]  (.getAnnotationTypeMemberDeclarations ^JavaProjectModel java-project-model)) models)
      :Type (mapcat (fn [java-project-model]  (.getTypes ^JavaProjectModel java-project-model)) models)
      ;;SimpleName, QualifiedName, FieldAccess, SuperFieldAccess
      :FieldAccessLike (mapcat (fn [java-project-model]  (.getFieldAccessLikeNodes ^JavaProjectModel java-project-model)) models)
      ;;MethodInvocation, SuperMethodInvocation, ClassInstanceCreation, ConstructorInvocation, SuperConstructorInvocation
      :MethodInvocationLike (mapcat (fn [java-project-model]  (.getInvocationLikeNodes ^JavaProjectModel java-project-model)) models)
      :FieldAccess (filter (fn [n] (instance? FieldAccess n)) (nodes-of-type :FieldAccessLike))
      :SuperFieldAccess (filter (fn [n] (instance? SuperFieldAccess n)) (nodes-of-type :FieldAccessLike))
      :MethodInvocation (filter (fn [n] (instance? MethodInvocation n)) (nodes-of-type :MethodInvocationLike))
      :SuperMethodInvocation (filter (fn [n] (instance? SuperMethodInvocation n)) (nodes-of-type :MethodInvocationLike))
      :ClassInstanceCreation (filter (fn [n] (instance? ClassInstanceCreation n)) (nodes-of-type :MethodInvocationLike))
      :ConstructorInvocation (filter (fn [n] (instance? ConstructorInvocation n)) (nodes-of-type :MethodInvocationLike))
      :SuperConstructorInvocation (filter (fn [n] (instance? SuperConstructorInvocation n)) (nodes-of-type :MethodInvocationLike))                           
      (try (subclass-nodes-of-type t) (catch ClassNotFoundException e [])))))
                                

      
; Unification with Maps (for now, just as a convenient shortcut, subject to change ... not meant for domain-specific unifications (constraints would be nicer there))
; ==================================================================================================================================================================

    ;DANGER: the following is only meant as a short-hand ... the second query will not work
    ;TODO: undo this extension --undone
    ;(ekeko* [?ast ?a ?j ?n] (ast :PackageDeclaration ?ast) (l/==  {:annotations ?a :javadoc ?j :name ?n} ?ast))
    ;(ekeko* [?ast ?a ?j ?n] (l/== ?ast {:annotations ?a :javadoc ?j :name ?n}) (ast :PackageDeclaration ?ast))



;definition of IUnifyWithASTNode
;(defprotocol IUnifyWithASTNode
;  (unify-with-ast [v u s]))

;extend double dispatch to ast nodes
;(extend-protocol IUnifyTerms
;  ASTNode
;  (unify-terms [u v s]
;    (unify-with-ast v u s)))

;implementing unification of v with ast
;(extend-protocol IUnifyWithASTNode
;  nil
;  (unify-with-ast [v ast s] false)
;  Object
;  (unify-with-ast [v ast s] false)
;  ASTNode
;  (unify-with-ast [v ast s] (if (= ast v) s false))
;  LVar
;  (unify-with-ast [v ast s] (ext-no-check s v ast))
;  clojure.lang.IPersistentMap
;  (unify-with-ast [v ast s]  
;              (let [reifs (reifiers ast)
;                    mp (zipmap (keys reifs) ;TODO: either ensure map is constructed lazily, or implement map unification as core.logic does
;                               (map (fn [f] (f)) (vals reifs)))] ;TODO: this can't be idiomatic
;                (unify-with-map v mp s))))

;implement unification of ast node v with map u
;(extend-protocol IUnifyWithMap
;  ASTNode
;  (unify-with-map [v u s] (unify-with-ast u v s)))

; Reification of Eclipse Java Model
; ---------------------------------



;; To be moved

(defn 
  name-string|qualified
  "Relation between a SimpleName or QualifiedName ASTNode and its fully qualified name as a string."
  [?name ?string]
  (l/all
    (ast :Name ?name)
    (el/equals ?string (.getFullyQualifiedName ^org.eclipse.jdt.core.dom.Name ?name))))

(defn
  name|simple-string
  "Relation between a SimpleName ASTNode and its identifier string."
  [?name ?string]
  (l/fresh [?wrappedstring]
    (ast :SimpleName ?name)
    (has :identifier ?name ?wrappedstring)
    (value-raw ?wrappedstring ?string)))

(defn
  name|qualified-string
  "Relation between a QualifiedName ASTNode and its identifier string."
  [?name ?string]
  (l/all 
    (ast :QualifiedName ?name)
    (el/equals ?string (.toString ^org.eclipse.jdt.core.dom.QualifiedName ?name))))

(defn
  name|qualified-name
  "Relation between a QualifiedName ASTNode and its last SimpleName part."
  [?qname ?sname]
  (l/all 
    (ast :QualifiedName ?qname) 
    (has :name ?qname ?sname)))

(defn
  name|qualified-qualifier
  "Relation between a QualifiedName ASTNode and its qualifier QualiefiedName part ."
  [?qname ?sname]
  (l/all 
    (ast :QualifiedName ?qname) 
    (has :qualifier ?qname ?sname)))

(defn
  name-name|same|qualified
  "Relation between two (Simple/Qualified)Name instances whose qualified name as a string is the same."
  [?name-a ?name-b]
  (l/fresh [?string]
    (name-string|qualified ?name-a ?string)
    (name-string|qualified ?name-b ?string)))

(defn
  name|simple-name|simple|same
  "Relation between two SimpleName instances that share the same identifier."
  [?name-a ?name-b]
  (l/fresh [?identifier]
         (name|simple-string ?name-a ?identifier)
         (name|simple-string ?name-b ?identifier)))

    
