(ns 
  ^{:doc "Low-level relations of JDT ASTNodes, IJavaElements and IBindings."
    :author "Coen De Roover"}
  damp.ekeko.jdt.reification
  (:refer-clojure :exclude [== type])
  (:use [clojure.core.logic])
  (:use [damp.ekeko logic])
  (:require 
    [damp.ekeko [ekekomodel :as ekekomodel]]
    [damp.ekeko.jdt [javaprojectmodel :as javaprojectmodel] [astnode :as astnode]])
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
     SuperFieldAccess FieldAccess ConstructorInvocation ASTNode ASTNode$NodeList CompilationUnit]))
     
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
  (conda [(v+ ?node) (conda [(v+ ?keyword) (all (succeeds-without-exception (instance? (astnode/class-for-ekeko-keyword ?keyword) ?node)))]
                            [(v- ?keyword) (all (succeeds (astnode/ast? ?node))
                                                (equals ?keyword (astnode/ekeko-keyword-for-class-of ?node)))])]
         [(v- ?node) (conda [(v+ ?keyword) (fresh [?nodes]
                                                  (equals ?nodes (nodes-of-type ?keyword))
                                                  (contains ?nodes ?node))]
                            [(v- ?keyword) (fresh [?keywords]
                                                  (contains astnode/ekeko-keywords-for-ast-classes ?keyword)
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
   (conda [(v+ ?node) (conda [(v+ ?keyword) 
                              (fresh [?childretrievingf]
                                    (equals ?childretrievingf (?keyword (astnode/reifiers ?node)))
                                    (!= ?childretrievingf nil)
                                    (equals ?child (?childretrievingf)))]
                             [(v- ?keyword) (fresh [?keywords]
                                                  (equals ?keywords (keys (astnode/reifiers ?node)))
                                                  (contains ?keywords ?keyword)
                                                  (has ?keyword ?node ?child))])]
          [(v- ?node) (conda [(v+ ?child) (all (equals ?node (astnode/owner ?child)) 
                                               (has ?keyword ?node ?child))]
                             [(v- ?child) (fresh [?astkeyw]
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
  (fresh [?ch] 
    (has ?keyword ?node ?ch)
    (conda
           [(succeeds (astnode/ast? ?ch))
            (== ?child ?ch)] 
           [(succeeds (astnode/lstvalue? ?ch))
            (contains (:value ?ch) ?child)])))
            
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
  (fresh [?keyw ?ch]
    (child ?keyw ?node ?ch)
    (conde [(== ?ch ?child)]
           [(child+ ?ch ?child)])))

;tabled version is much slower (169294ms vs 5990ms on jHotDraw)
;but might be faster if multiple child+ conditions are used in a query

;(def tabled-child+ 
;  (tabled [?node ?child]
;   (fresh [?keyw ?ch]
;          (child ?keyw ?node ?ch)
;          (conde [(== ?ch ?child)]
;                [(tabled-child+ ?ch ?child)]))))    
  

(defn
  value
  "Relation of ASTNode property values that aren't ASTNode themselves:
   nil, primitive values and lists."
  [?val]
  (conda [(v+ ?val)
          (succeeds (astnode/value? ?val))]
         [(v- ?val)
          (fresh [?kind ?ast ?property]
                 (ast ?kind ?ast)
                 (has ?property ?ast ?val)
                 (value ?val))]))

(defn
  nullvalue
  "Relation of all null-valued ASTNode property values."
  [?val]
  (all
    (value ?val)
    (succeeds (astnode/nilvalue? ?val))))

(defn
  listvalue
  "Relation of all list-valued ASTNode property values."
  [?val]
  (all
    (value ?val)
    (succeeds (astnode/lstvalue? ?val))))

(defn
  primitivevalue
  "Relation of all primitive-valued ASTNode property values."
  [?val]
  (all
    (value ?val)
    (succeeds (astnode/primitivevalue? ?val))))

(defn
  value-raw
  "Relation of an ASTNode property value that isn't an ASTNode itself
   and its raw value (either a primitive, nil, or a list)."
  [?val ?raw]
  (all
    (value ?val)
    (equals ?raw (:value ?val))))
          
(defn
  ast-parent
  "Relation between an ASTNode instance ?ast and its parent node 
   ?parent. Note that ?parent is nil when ?ast is the root AST node 
   (i.e., a CompilationUnit instance).
   
   See also:
   API documentation of org.eclipse.jdt.core.dom.ASTNode" 
  [?ast ?parent]
  (fresh [?key]
    (ast ?key ?ast)
    (equals ?parent (.getParent ^ASTNode ?ast))))

(defn 
  ast-root
  "Relation between an ASTNode instance ?ast and the root of its tree ?cu,
   an instance of CompilationUnit.

   See also:
   API documentation of org.eclipse.jdt.core.dom.ASTNode"
  [?ast ?cu]
  (fresh [?key]
    (ast ?key ?ast)
    (equals ?cu (.getRoot ^ASTNode ?ast))))

(defn 
  ast-resolveable 
  "Like binary ast/2 predicate, but ensures ?ast is an ASTNode
   instance that can be resolved to an IBinding 
   (i.e., implements method resolveBinding).

   See also:
   Binary predicate ast/2
   API documentation of org.eclipse.jdt.core.dom.IBinding"
  [?key ?ast]
  (let  [keys astnode/ekeko-keywords-for-resolveable-ast-classes]
    (all
         (contains keys ?key)
         (ast ?key ?ast))))

(defn 
  ast-declaration 
   "Like binary ast/2 predicate, but ensures ?ast is
    a declaration ASTNode instance.

   See also:
   Binary predicate ast/2"
  [?key ?ast]
  (let [declaration-keywords astnode/ekeko-keywords-for-declaration-ast-classes]
    (all 
      (contains declaration-keywords ?key)
      (ast ?key ?ast))))

;TODO: remove pseudo-node keywords 
(defn 
  ast-fieldaccess
   "Like binary ast/2 predicate, but ensures ?ast is 
    an ASTNode that accesses a field.

    Note that ?ast can be an instance of FieldAccess, 
    SuperFieldAccess, SimpleName or QualifiedName."
  [?key ?node] 
  (conde [(v- ?node)
          (ast :FieldAccessLike ?node) 
          (ast ?key ?node)]
         [(v+ ?node) 
          (fresh [?n] 
                 (ast :FieldAccessLike ?n)
                 (== ?n ?node))]))
    
(defn 
  ast-type
  "Like ast/2, but ensures ?type is an 
   instance of one of the Type subclasses.
   Note that these are references to types within the source code, 
   rather than a canonical representation of a type."
  [?key ?type] 
  (all 
    (ast :Type ?type) ;above trick is not necessary as Type is an actual super class 
    (ast ?key ?type)))


(defn 
  ast-resolveable-declaration
   "Like binary ast/2 predicate, but ensures ?ast 
    is a resolveable declaration ASTNode instance.

   See also:
   Binary predicates ast-resolveable/2 and ast-declaration/2."
  [?key ?ast]
  (all 
    (ast-declaration ?key ?ast)
    (ast-resolveable ?key ?ast)))

(defn 
  ast-invocation 
  "Like binary ast/2, but ensures ?ast is an ASTNode 
   that invokes a method or constructor.

   Note that ?ast can be an instance of MethodInvocation, 
   SuperMethodInvocation, ClassInstanceCreation,
   ConstructorInvocation or SuperConstructorInvocation"
  [?key ?node] 
  (conde [(v- ?node)
          (ast :MethodInvocationLike ?node)
          (ast ?key ?node)]
         [(v+ ?node)
          (ast ?key ?node)
          (contains [:MethodInvocation 
                     :SuperMethodInvocation
                     :ClassInstanceCreation 
                     :ConstructorInvocation 
                     :SuperConstructorInvocation] ?key) ;TODO: define a constant for this
          ]))
  

(declare ast-expression-typebinding)

(defn 
  ast-expression-reference
  "Relation between a reference-valued Expression instance ?ast and 
   the keyword ?keyw representing its kind."
  [?keyw ?ast]
  (fresh [?binding]
         (ast-expression-typebinding ?keyw ?ast ?binding)
         (succeeds (not (.isPrimitive ^ITypeBinding ?binding))))) 


(defn 
  ast-name-of-parameter
  "Like ast/2, but ensures ?name is the SimpleName 
   of a formal parameter declaration."
  [?keyw ?name]
  (fresh [?nameBinding ?parent]
         (== ?keyw :SimpleName)
         (ast ?keyw ?name) 
         (equals ?nameBinding (.resolveBinding ^SimpleName ?name))
         (succeeds (.isParameter ^IVariableBinding ?nameBinding))
         (ast-parent ?name ?parent)
         (ast :SingleVariableDeclaration ?parent)))


;; Bindings
;; --------

(defn
  ast-expression-typebinding
  "Relation between an Expression instance ?ast,
   the keyword ?key representing its kind,
   and the ITypeBinding ?binding for its type."
  [?key ?ast ?binding]
  (all
    (ast :Expression ?ast)
    (equals ?binding (.resolveTypeBinding ^Expression ?ast))
    (!= nil ?binding)
    (ast ?key ?ast)))


(defn 
  ast-type-binding
   "Relation between a type ASTNode ?type, the keyword ?key 
    corresponding to its kind, and the ITypeBinding it resolves to.
   
   See also: 
   binary predicate ast-type/2"

  [?key ?type ?binding]
  (all 
    (ast-type ?key ?type)
    (!= nil ?binding)
    (equals ?binding (.resolveBinding ^Type ?type))))


(defn
  ast-typedeclaration-binding
  "Relation between a TypeDeclaration instance ?typeDeclaration
   and the ITypeBinding instance ?binding it resolves to.
   Note that ?binding is required to differ from ?nil. 
   
   See also:
   API documentation of org.eclipse.jdt.core.dom.TypeDeclaration
   and org.eclipse.jdt.core.dom.ITypeBinding"
  [?key ?typeDeclaration ?binding]
  (all 
    (== ?key :TypeDeclaration)
    (ast ?key ?typeDeclaration)
    (!= nil ?binding)
    (equals ?binding (.resolveBinding ^TypeDeclaration ?typeDeclaration))))

(defprotocol 
  IResolveToFieldBinding
  (binding-for-fieldaccesss-like-node [n]))

(extend-protocol 
  IResolveToFieldBinding
  FieldAccess
  (binding-for-fieldaccesss-like-node [n] (.resolveFieldBinding n)) 
  SuperFieldAccess
  (binding-for-fieldaccesss-like-node [n] (.resolveFieldBinding n))
  SimpleName
  (binding-for-fieldaccesss-like-node [n] (.resolveBinding n))
  QualifiedName
  (binding-for-fieldaccesss-like-node [n] (.resolveBinding n)))

(defn
  ast-fieldaccess-binding
  "Relation between a field accessing ASTNode ?node, the keyword 
   ?key corresponding to its kind, and the IVariableBinding it resolves to.
   
   See also: 
   binary predicate ast-fieldaccess/2"
  [?key ?node ?binding]
  (all 
    (ast-fieldaccess ?key ?node)
    (!= nil ?binding)
    (equals ?binding (binding-for-fieldaccesss-like-node ?node))))

(defn 
  ast-declares-binding
  "Relation between a declaration ASTNode ?n,
   the keyword ?key corresponding to its kind, and the IBinding it resolves to.
   
   Note that this relation is quite slow to compute.

   See also:
   ternary predicate ast-typedeclaration-binding/3 
   which restricts ?n to TypeDeclaration instances"
  [?key ?n ?binding]
  (all 
    (!= ?binding nil)
    (!= ?n nil)
    (conda [(v+ ?binding) (all
                            (equals ?n (javaprojectmodel/binding-to-declaration ?binding))
                            (ast ?key ?n))]
           [(v- ?binding) (all
                            (ast-resolveable-declaration ?key ?n)
                            (equals ?binding (.resolveBinding ?n)))]))) ;no type hint possible, no common super class for nodes with such a method
   
(defprotocol 
  IResolveToMethodBinding
  (binding-for-invocation-like-node [n]))

(extend-protocol 
  IResolveToMethodBinding
  MethodInvocation
  (binding-for-invocation-like-node [n] (.resolveMethodBinding n)) 
  SuperMethodInvocation
  (binding-for-invocation-like-node [n] (.resolveMethodBinding n))
  ClassInstanceCreation
  (binding-for-invocation-like-node [n] (.resolveConstructorBinding n))
  ConstructorInvocation
  (binding-for-invocation-like-node [n] (.resolveConstructorBinding n))
  SuperConstructorInvocation
  (binding-for-invocation-like-node [n] (.resolveConstructorBinding n)))
    
(defn 
  ast-invocation-binding
  "Relation between an ASTNode ?node that invokes a method or constructor, 
   the keyword ?key corresponding to its kind, 
   and the IMethodBinding it resolves to.
   
   See also: 
   binary predicate ast-invocation/2"
  [?key ?node ?binding]
  (all 
    (ast-invocation ?key ?node)
    (!= nil ?binding)
    (equals ?binding (binding-for-invocation-like-node ?node))))



;; Control Flow Graph
;; ------------------


(defn 
  method-statements
  "Relation between a MethodDeclaration ?m and its
   ASTNode$NodeList list of statements ?slist in its body. 
   Note that abstract methods are not included in this relation, 
   but methods with en empty body are."
  [?m ?statements]
  (fresh [?body ?slist]
         (ast :MethodDeclaration ?m)
         (has :body ?m ?body)
         (!= nil ?body)
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
                (all
                  (project [node]
                           (== to (seq (.get ^Map (.predecessors ^ControlFlowGraph jdt-cfg) node))))))
   :successors (fn 
                [node to]
                (all
                  (project [node]
                           (== to (seq (.get ^Map (.successors ^ControlFlowGraph jdt-cfg) node))))))})
 
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
           (fresh [?beforeReturn ?return]
                  (project [?cfg]
                           (qwal ?cfg ?entry ?end 
                                 []
                                 (qcurrent [currentStatement] (equals currentStatement ?beforeReturn))
                                 q=>
                                 (qcurrent [currentStatement] (equals currentStatement ?return) (ast :ReturnStatement ?return))))))

  See also:
  Documentation of the damp.qwal library.
  Predicates method-cfg-entry and method-cfg-exit which quantify 
  over the symbolic entry and exit point of a method's control flow graph."
  [?m ?cfg]
  (fresh [?slist]
        (method-statements ?m ?slist)
        (succeeds (> (.size ^ASTNode$NodeList ?slist) 0)) 
        (equals ?cfg (qwal-graph-from-jdt-cfg (jdt-method-cfg ?m)))))

(defn-
  method-first-statement
  [?m ?s]
  (fresh [?slist]
       (method-statements ?m ?slist)
       (succeeds (> (.size ^ASTNode$NodeList ?slist) 0)) 
       (equals ?s (first ?slist))))       

(defn 
  method-cfg-entry
  "Relation between MethodDeclaration ?m and the statement ?entry 
   that represents the entry point of its control flow graph."
  [?m ?entry]
  (all 
    (method-first-statement ?m ?entry)))

(defn 
  method-cfg-exit 
  "Relation between MethodDeclaration ?m and the Block ?exit that
   represents the symbolic exit point of its control flow graph.

  See also:
  Predicate method-cfg/2 and documentation of the damp.qwal library."
  [?m ?exit]
  (all 
   (ast :MethodDeclaration ?m)
   (has :body ?m ?exit)
   (!= nil ?exit)))




; Node generators called by reification goals
; -------------------------------------------

(defn- subclass-nodes-of-type [t]
  (let [c (astnode/class-for-ekeko-keyword t)
        s (supers c)]
    (cond (contains? s org.eclipse.jdt.core.dom.Expression)
            (filter (fn [n] (instance? c n))
              (nodes-of-type :Expression))
          (contains? s org.eclipse.jdt.core.dom.Statement)
            (filter (fn [n] (instance? c n))
              (nodes-of-type :Statement))
          (contains? s org.eclipse.jdt.core.dom.Type)
            (filter (fn [n] (instance? c n))
              (nodes-of-type :Type))
          :else 
            (run* [?n] 
              (fresh [?cu]
                (ast :CompilationUnit ?cu)
                (child+ ?cu ?n)
                (succeeds (instance? c ?n)))))))

(defn- logic-all-import-declarations []
  (run* [?importDeclaration]
        (fresh [?cu]
               (ast :CompilationUnit ?cu)
               (child :imports ?cu ?importDeclaration))))

(defn- logic-all-package-declarations []
  (run* [?packageDeclaration]
        (fresh [?cu]
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
    ;(ekeko* [?ast ?a ?j ?n] (ast :PackageDeclaration ?ast) (==  {:annotations ?a :javadoc ?j :name ?n} ?ast))
    ;(ekeko* [?ast ?a ?j ?n] (== ?ast {:annotations ?a :javadoc ?j :name ?n}) (ast :PackageDeclaration ?ast))



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


(defn- 
  element-ekeko-model 
  [?element ?ekeko-model]
  (all
    (equals ?ekeko-model (.getJavaProjectModel 
                           ^damp.ekeko.EkekoModel
                           (ekekomodel/ekeko-model) 
                           (.getJavaProject ^IJavaElement ?element)))))

;NOTE: tabling cannot be used here because typehierarchies are static 
;the ekeko model installs a listener for changes in the typehierarchy
(defn-
  itype-supertypehierarchy ;TODO: same for complete hierarchy, but those are presumably more expensive to construct and maintain
  [?itype ?typeHierarchy]
  (all
    (equals ?typeHierarchy (.getTypeHierarchy ^damp.ekeko.EkekoModel (ekekomodel/ekeko-model) ?itype))))


;Note that the JDT does not consider java.lang.Object to be a supertype of any interface type.
;Returns all resolved supertypes of the given type, in bottom-up order. 
(defn-
  itype-super-itypes
  [?itype ?supers]
  (fresh [?hierarchy]
         (itype-supertypehierarchy ?itype ?hierarchy)
         (equals ?supers (.getAllSupertypes ^ITypeHierarchy ?hierarchy ?itype))))


(declare type)

(defn 
  type-super-type
  "Successively unifies ?itype with every 
   supertype of the given IType ?itype, in bottom-up order. 

   Note that the JDT does not consider java.lang.Object
   to be a supertype of any interface type."
  [?itype ?super-itype]
  (fresh [?supers]
     (type ?itype)
     (itype-super-itypes ?itype ?supers)
     (contains ?supers ?super-itype)))



(defn- ibinding-for-ijavaelement [^IJavaElement ijavaelement]
  (let [^ASTParser parser (ASTParser/newParser AST/JLS4)]                
    (.setProject  parser (.getJavaProject ijavaelement))
    (if-let [bindings (.createBindings parser (into-array IJavaElement [ijavaelement]) nil)]
      (first bindings))))
  
  
(defn 
  element-binding
  "Non-relational. Unifies ?ibinding with the IBinding 
   corresponding to the IJavaElement ?ijavaelement, if there is one.

  See also:
  Binary predicate binding-element/2" 
  [?ijavaelement ?ibinding]
  (all 
    (!= nil ?ibinding)
    (equals ?ibinding (ibinding-for-ijavaelement ?ijavaelement))))        


(defn 
  binding-element
  "Non-relational. Unifies ?ijavaelement with the IJavaElement
   corresponding to the IBinding ?ibinding, if there is one.

  See also:
  Binary predicate element-binding/2" 
  [?ibinding ?ijavaelement]
  (all
    (!= nil ?ijavaelement)
    (equals ?ijavaelement (.getJavaElement ^IBinding ?ibinding))))


;; Java Model Reification
;; ----------------------

(defn
  packagefragmentroot
  "Relation of IPackageFragmentRoot instances ?r."
  [?r]
  (let [roots (mapcat javaprojectmodel/javaproject-packagefragmentroots
                      (javaprojectmodel/ekeko-javaprojects))]
    (all
      (contains roots ?r))))
  
(defn 
  packagefragmentroot-from-binary
  "Relation of IPackageFragmentRoot instances ?r that originate from byte code."
  [?r]
  (all
    (packagefragmentroot ?r)
    (succeeds (javaprojectmodel/packagefragmentroot-binary? ?r))))


(defn 
  packagefragmentroot-from-source
  "Relation of IPackageFragmentRoot instances ?r that originate from source code."
  [?r]
  (all
    (packagefragmentroot ?r)
    (succeeds (javaprojectmodel/packagefragmentroot-source? ?r))))


(defn
  packagefragmentroot-fragment
  "Relation between a IPackageFragmentRoot ?r and one of its IPckageFragment instances ?p." 
  [?r ?f]
  (all 
    (packagefragmentroot ?r)
    (contains (javaprojectmodel/packagefragmentroot-fragments ?r) ?f)))


(defn
  packagefragment 
  "Relation of IPackageFragment instances."
  [?f]
  (fresh [?r]
         (packagefragmentroot-fragment ?r ?f)))

(defn
  packagefragment-name
  "Relation between an IPackageFragment ?f and its name String ?n." 
  [?f ?n]
  (fresh [?r]
         (packagefragmentroot-fragment ?r ?f)
         (equals ?n (javaprojectmodel/packagefragment-name ?f))))
  

(defn 
  packagefragment-classfile
  "Relation between an IPackageFragment ?f and one of its IClassFile instances ?c."
  [?f ?c]
  (all
    (packagefragment ?f)
    (contains (.getClassFiles ^IPackageFragment ?f) ?c)))

(defn 
  classfile
  "Relation of IClassFile instances ?c."
  [?c]
  (fresh [?p]
         (packagefragment-classfile ?p ?c)))

(defn 
  packagefragment-compilationunit
  "Relation between an IPackageFragment ?f and one of its ICompilationUnit instances ?c."
  [?f ?c]
  (all
    (packagefragment ?f)
    (contains (.getCompilationUnits ^IPackageFragment ?f) ?c)))

(defn 
  compilationunit
  "Relation of ICompilationUnit instances ?c"
  [?c]
  (fresh [?p]
         (packagefragment-compilationunit ?p ?c)))

(defn 
  classfile-type
  "Relation between an IClassFile ?c and the IType ?t it declares."
  [?c ?t]
  (all
    (classfile ?c)
    (equals ?t (.getType ^IClassFile ?c))))

(defn 
  compilationunit-type
  "Relation between an ICompilationUnit ?c and one of the top-level IType instances ?t it declares."
  [?c ?t]
  (all
    (compilationunit ?c)
    (contains (.getTypes ^ICompilationUnit ?c) ?t)))



(declare type-membertype)

(defn
  type
  "Relation of IType instances ?t." 
  [?t]
  (conda [(v+ ?t) 
          (succeeds (instance? IType ?t))]
         [(v- ?t) 
          (conde
            [(fresh [?classfile]
                    (classfile-type ?classfile ?t))]
            [(fresh [?compilationunit ?toplevelt]
                    (compilationunit-type ?compilationunit ?toplevelt)
                    (conde [(== ?toplevelt ?t)]
                           [(type-membertype ?toplevelt ?t)]))])]))


(defn
  type-from-source
  "Relation of IType instances ?t that originate from a source file."
  [?t]
  (all 
    (type ?t)
    (equals false (.isBinary ^IType ?t))))

(defn
  type-from-binary
  "Relation of IType instances ?t that originate from a binary file."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isBinary ^IType ?t))))


(defn
  type-membertype
  "Relation of IType ?t and one of its immediate member types ?mt."
  [?t ?mt]
  (all
    (type ?t)
    (contains (.getTypes ^IType ?t) ?mt)))

(defn
  type-simplename
  "Relation of IType ?t and its simple name String ?n."
  [?t ?n]
  (all
    (type ?t)
    (equals ?n (.getElementName ^IType ?t))))


(defn
  type-qualifiedname
  "Relation of IType ?t and its fully qualified name String ?n."
  [?t ?n]
  (conda 
    [(v+ ?t)
     (succeeds (instance? IType ?t))
     (equals ?n (.getElementName ^IType ?t))]
    [(v- ?t)
     (conda [(v- ?n) 
             (type ?t)
             (type-qualifiedname ?t ?n)]
             [(v+ ?n)
              (fresh [?types]
                     (equals ?types (into #{}
                                          (map 
                                            (fn [p]
                                              (let [^WorkingCopyOwner wco nil
                                                    ^IProgressMonitor pm nil]
                                                (.findType ^IJavaProject p ^String ?n wco pm)))
                                            (javaprojectmodel/ekeko-javaprojects))))
                     (contains ?types ?t)
                     (!= nil ?t))])]))

(defn 
  ast-type-type
  "Relation between a type ASTNode ?ast, its keyword kind ?key, and the IType ?type it refers to.

  See also:
  ast-type-binding/3 which resolves a type ASTNode to an ITypeBinding."
  [?key ?ast ?type]
  (fresh [?binding]
         (ast-type-binding ?key ?ast ?binding)
         (binding-element ?binding ?type)))


(defn
  type-initializer
  "Relation between an IType ?t and one of its IInitializer initializers ?i.

   Note that their are none for binary types."
  [?t ?i]
  (all
    (type ?t)
    (contains (.getInitializers ^IType ?t) ?t)))

(defn
  initializer
  "Relation of type initializers."
  [?i]
  (fresh [?t]
         (type-initializer ?t ?i)))


(defn
  type-method
  "Relation between IType ?t and one if its declared IMethod instances ?m."
  [?t ?m]
  (all
    (type ?t)
    (contains (.getMethods ^IType ?t) ?m)))

(defn 
  method
  "Relation of IMethod instances.

  See also: 
  method-from-source/1 and method-from-binary/1."
  [?m]
  (fresh [?t]
         (type-method ?t ?m)))

(defn
  method-from-source
  "Relation of IMethod instances declared in a source type.

  Note that (ast :MethodDeclaration ?m) is much more efficient
  and corresponds to the relation of method ASTs."
  [?m]
  (fresh [?t]
         (type-from-source ?t)
         (type-method ?t ?m)))

(defn 
  method-from-binary
  "Relation of IMethod instances declared in a binary type."
  [?m]
  (fresh [?t]
         (type-from-binary ?t)
         (type-method ?t ?m)))



(defn
  type-field
  "Relation between IType ?t and one if its declared IField instances ?f."
  [?t ?f]
  (all
    (type ?t)
    (contains (.getFields ^IType ?t) ?f)))

(defn 
  field
  "Relation of IField instances.

  See also: 
  field-from-source/1 and field-from-binary/1."
  [?f]
  (fresh [?t]
         (type-field ?t ?f)))

(defn
  field-from-source
  "Relation of IField instances declared in a source type."
  [?f]
  (fresh [?t]
         (type-from-source ?t)
         (type-field ?t ?f)))

(defn 
  field-from-binary
  "Relation of IField instances declared in a binary type."
  [?f]
  (fresh [?t]
         (type-from-binary ?t)
         (type-field ?t ?f)))


;TODO: method, field names



(defn 
  element
  "Relation of IJavaElement instances ?element and the keyword ?key representing their kind."
  [?key ?element]
  (conda
    [(v+ ?element)
     (succeeds (instance? IJavaElement ?element))
     (equals ?key (astnode/ekeko-keyword-for-class-of ?element))]
    [(v- ?element)
     (conda
       [(== ?key :PackageFragmentRoot)
        (packagefragmentroot ?element)]
       [(== ?key :PackageFragment)
        (fresh [?root]
               (packagefragmentroot ?root)
               (packagefragmentroot-fragment ?root ?element))]
       [(== ?key :CompilationUnit)
        (compilationunit ?element)]
        [(== ?key :ClassFile)
         (classfile ?element)]
        [(== ?key :Type)
         (type ?element)]
        [(== ?key :Initializer)
         (initializer ?element)]
        [(== ?key :Method)
         (method ?element)]
        [(== ?key :Field)
         (field ?element)]
        )]))
       
(defn
  type-typeparameters 
  "Relation between an IType ?t and its formal type parameters ITypeParameter[] ?ps."
  [?t ?ps]
  (all
    (type ?t)
    (equals ?ps (.getTypeParameters ^IType ?t))))

(declare method)
(defn
  method-typeparameters
  "Relation between an IMethod ?m and its formal type parameters ITypeParameter[] ?ps."
  [?m ?ps]
  (all
    (method ?m)
    (equals ?ps (.getTypeParameters ^IMethod ?m))))


(defn
  element-typeparameter
  "Relation of an IMethod or IType element ?e and one its formal ITypeParameter instances ?t."
  [?e ?t]
  (fresh [?ts]
         (conde [(type-typeparameters ?e ?ts)]
                [(method-typeparameters ?e ?ts)])
         (contains ?ts ?t)))

    

