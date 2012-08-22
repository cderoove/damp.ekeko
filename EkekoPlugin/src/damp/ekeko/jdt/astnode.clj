(ns damp.ekeko.jdt.astnode
  (:import 
    [java.lang Class]
    [java.lang.reflect Field]
    [org.eclipse.jdt.core.dom ASTNode 
                              ASTNode$NodeList 
                              CompilationUnit 
                              StructuralPropertyDescriptor ChildPropertyDescriptor ChildListPropertyDescriptor SimplePropertyDescriptor
                              Statement
                              Expression
                              Name
                              QualifiedName
                              Annotation
                              IBinding
                              Block EnhancedForStatement ForStatement IfStatement LabeledStatement SwitchStatement
                              DoStatement SynchronizedStatement TryStatement WhileStatement]
                              ))


(set! *warn-on-reflection* true)

;; JDT DOM classes
;; ---------------

(defn 
  node-classes
  "Returns a Seq of all known ASTNode subclasses."
  []
  (let [lasttypeint 84]
    (for [i (range 1 (inc lasttypeint))]
      (ASTNode/nodeClassForType i))))

(def 
  node-classes-as-symbols 
  (memoize (fn []
             (map (fn [^Class c] (symbol (.getSimpleName c))) (node-classes)))))

(def class-for-ekeko-keyword 
  (memoize (fn 
             [classkeyword]
             (Class/forName (str "org.eclipse.jdt.core.dom." (name classkeyword))))))

(def ekeko-keyword-for-class 
  (memoize (fn
             [astclass]
             (keyword (.getSimpleName ^Class astclass)))))

(defn ekeko-keyword-for-class-of [astnode]
 (ekeko-keyword-for-class (class astnode)))

(def resolveable-node-classes
  (memoize (fn 
             []
             (filter (fn [^Class c] 
                       (try 
                         (let [m (.getMethod c "resolveBinding" (make-array java.lang.Class 0))]
                           true)
                         (catch NoSuchMethodException e false))) 
                     (node-classes)))))

(defn ekeko-keywords-for-resolveable-ast-classes []
  (map ekeko-keyword-for-class (resolveable-node-classes)))

(defn ekeko-keywords-for-declaration-ast-classes []
  (filter (fn [keyw] 
            (re-matches #".*Declaration" (str keyw)))
          (map ekeko-keyword-for-class (node-classes))))
    

;; Property descriptors
;; --------------------

(defn property-descriptor-id [^StructuralPropertyDescriptor p]
  (.getId p))

(defn property-descriptor-simple? [^StructuralPropertyDescriptor p]
  (.isSimpleProperty p))

(defn property-descriptor-list? [^StructuralPropertyDescriptor p]
  (.isChildListProperty p))

(defn property-descriptor-child? [^StructuralPropertyDescriptor p]
  (.isChildProperty p))

(defn ^Class property-descriptor-owner-node-class [^StructuralPropertyDescriptor p]
  (.getNodeClass p))

(defn property-descriptor-child-node-class [^ChildPropertyDescriptor p]
  (.getChildType p))

(defn property-descriptor-element-node-class [^ChildListPropertyDescriptor p]
  (.getElementType p))

(defn property-descriptor-value-class [^SimplePropertyDescriptor p]
  (.getValueType p))

(defn ekeko-keyword-for-property-descriptor [p] 
  (keyword (property-descriptor-id p)))

(defn node-property-descriptors [^ASTNode n] 
  (.structuralPropertiesForType n))

(defn node-property-descriptor-for-ekeko-keyword [^ASTNode n k]
  (first (filter (fn [d] (= k (ekeko-keyword-for-property-descriptor d))) 
                   (node-property-descriptors n))))

(defn node-property-value [^ASTNode n ^StructuralPropertyDescriptor p]
  (.getStructuralProperty n p))

;.-notation works on literals, not on variables holding a java.lang.class instance
;can be used as: (damp.ekeko.ast/nodeclass-property-descriptors (class cu)) where cu is a cu node
(defn nodeclass-property-descriptors [^Class cls]
  (clojure.lang.Reflector/invokeStaticMethod cls "propertyDescriptors" (to-array [org.eclipse.jdt.core.dom.AST/JLS3])))

(def property-descriptors-per-node-class
  (memoize 
    (fn []
      (let [classes (node-classes)]
        (zipmap classes
                (map (fn [c] (nodeclass-property-descriptors c))
                     classes))))))

(def owner-properties-per-node-class
  (memoize 
    (fn 
      []
      (let [allproperties (apply concat (vals (property-descriptors-per-node-class)))
            ownerperclass (atom {})]
        (doseq [p allproperties] ;not simpelvalue
          ; (when (not (property-descriptor-simple? p))
          (swap! ownerperclass (fn [oldmap]
                                 (merge-with concat
                                             ;concat is to be used when a value already exists for a key
                                             oldmap
                                             {((cond
                                                 (property-descriptor-simple? p) property-descriptor-value-class
                                                 (property-descriptor-child? p) property-descriptor-child-node-class
                                                 (property-descriptor-list? p) property-descriptor-element-node-class)
                                                p) ;key
                                              [
                                               ;[(ekeko-keyword-for-property-descriptor p)
                                               ; (ekeko-keyword-for-class (property-descriptor-owner-node-class p))]
                                               p
                                               ] ;value to be concatenated
                                              }))))
        @ownerperclass))))
  
  ;(def owner-keywords-per-node-keyword 
;  (into {} (for [[^Class k v] (owners-per-node-class)]
;             [(ast-keyword-for-class k)
;              (map (fn [^Class c] (ast-keyword-for-class c)) v)])))



;; Ekeko-specific properties
;; -------------------------

;it is supposedly faster to use protocols as this is simply single dispatch
;(defmulti ekeko-properties class)
;(defmethod ekeko-properties ASTNode [n] 
;  (.structuralPropertiesForType n))
;(defmethod ekeko-properties MethodDeclaration)

(defn node-ekeko-properties [n]
  (let [descriptors (node-property-descriptors n)]
    (zipmap (map (fn [^StructuralPropertyDescriptor p] (keyword (property-descriptor-id p))) descriptors)
            (map (fn [^StructuralPropertyDescriptor p] (fn [] (node-property-value n p))) descriptors))))

;from former (ns clojure.contrib.reflect)
(defn get-invisible-field
  "Access to private or protected field.  field-name is a symbol or
  keyword."
  [klass field-name obj]
  (-> ^Class klass ^Field (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

(defn owner [n-or-nlist]
  (if 
    (instance? ASTNode n-or-nlist)
    (.getParent ^ASTNode n-or-nlist)
    ;outer instance of nodelist is owner
    (get-invisible-field (class ^ASTNode$NodeList n-or-nlist) (symbol "this$0") n-or-nlist)))

(defn owner-property [n-or-nlist]
   (if 
     (instance? ASTNode n-or-nlist)
     (.getLocationInParent ^ASTNode n-or-nlist)
     (get-invisible-field (class ^ASTNode$NodeList n-or-nlist) 'propertyDescriptor n-or-nlist)))

(defprotocol IAST
  (reifiers [this] 
    "Returns a map of keywords to reifier functions. The latter will return an Ekeko-specific child of the AST node."))

(extend org.eclipse.jdt.core.dom.ASTNode
  IAST
  {:reifiers (fn [this] (node-ekeko-properties this))})
                          
(defn node-children [n]
  (map (fn [p] (node-property-value n p))
       (node-property-descriptors n)))

; Bindings
; --------


(defmacro resolved-binding [node]
  `(.resolveBinding ~node))

(defn binding-kind [^IBinding b]
  (.getKind b))

(defn binding-variable? [^IBinding b]
  (= IBinding/VARIABLE (binding-kind b)))

(defn binding-type? [^IBinding b]
  (= IBinding/TYPE (binding-kind b)))

(defn binding-package? [^IBinding b]
  (= IBinding/PACKAGE (binding-kind b)))

(defn binding-method? [^IBinding b]
  (= IBinding/METHOD (binding-kind b)))

(defn binding-annotation? [^IBinding b]
  (= IBinding/ANNOTATION (binding-kind b)))

(defn binding-member-value-pair? [^IBinding b]
  (= IBinding/MEMBER_VALUE_PAIR (binding-kind b)))

;;Very expensive; should not be used
;(defn ast-bindings [] 
;  (let [resolveable-asts
;       (reduce (fn [nodes keyword] (clojure.set/union nodes (set (nodes-of-type keyword))))
;               #{}
;               (ekeko-keywords-for-resolveable-ast-classes))]
;    (map (fn [ast] (.resolveBinding ast)) resolveable-asts)))


; Helper Predicates
; -----------------

(defmacro expression? [node]
  `(instance? Expression ~node))

;redundant: the ones in the expression table of the model ought to be the same
(defn actual-expression? [^ASTNode node]
  (and (expression? node)
       ;(not (instance? Annotation node)) ;TODO: I excluded Annotations from Cava's expressions
       (not (and (instance? Name node)
                 (or (= QualifiedName/QUALIFIER_PROPERTY (.getLocationInParent node)) ;no qualifiers of qualified names
                     (if-let [b (resolved-binding ^Name node)]
                       (not (binding-variable? b)) ;no names of methods etc..
                       false)))))) 

(defmacro statement? [node]
  `(instance? Statement ~node))

;TODO: ugly
(defn statement-with-scope? [node]
  (or (instance? Block node)
      (instance? IfStatement node)
      (instance? WhileStatement node)
      (instance? ForStatement node)
      (instance? EnhancedForStatement node)
      (instance? SynchronizedStatement node)
      (instance? TryStatement node)
      (instance? DoStatement node)
      (instance? LabeledStatement node)
      (instance? SwitchStatement node)))




;;Methods
(defn has-same-body? [method1 method2]
  (.subtreeMatch
    method1
    (new org.eclipse.jdt.core.dom.ASTMatcher)
    method2))


