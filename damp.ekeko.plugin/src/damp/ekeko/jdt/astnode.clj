(ns damp.ekeko.jdt.astnode
  (:require [damp.util [interop :as interop]])
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
                              DoStatement SynchronizedStatement TryStatement WhileStatement
                              MethodDeclaration]
                              ))

(set! *warn-on-reflection* true)



; Helper Predicates
; -----------------

(defn 
  ast?
  [x]
  (instance? ASTNode x))

(defn
  value?
  [x]
  (and 
    (map? x)
    (= :Value (:type x))))

(defn
  lstvalue?
  [x]
  (and
    (value? x)
    (instance? java.util.List (:value x))))
  

(defn
  nilvalue?
  [x]
  (and
    (value? x)
    (nil? (:value x))))

(defn
  primitivevalue?
  [x]
  (and
    (value? x)
    (not (instance? java.util.List (:value x)))
    (not (nil? (:value x)))))
  

(defn
  expression?
  [node]
  (instance? Expression node))

(defn
  statement?
  [node]
  (instance? Statement node))



;; JDT DOM classes
;; ---------------

(def 
  node-classes
  (let [lasttypeint 84]
    (for [i (range 1 (inc lasttypeint))]
      (ASTNode/nodeClassForType i))))

(def 
  node-classes-as-symbols 
  (map (fn [^Class c] (symbol (.getSimpleName c))) node-classes))

(def 
  class-for-ekeko-keyword 
  (memoize (fn 
             [classkeyword]
             (Class/forName (str "org.eclipse.jdt.core.dom." (name classkeyword))))))

(def 
  ekeko-keyword-for-class 
  (memoize (fn
             [astclass]
             (keyword (.getSimpleName ^Class astclass)))))

(defn 
  ekeko-keyword-for-class-of 
  [astnode]
  (ekeko-keyword-for-class (class astnode)))

(def
  ekeko-keywords-for-ast-classes
  (map ekeko-keyword-for-class node-classes))

(def 
  resolveable-node-classes
  (filter (fn [^Class c] 
            (try 
              (let [m (.getMethod c "resolveBinding" (make-array java.lang.Class 0))]
                true)
              (catch NoSuchMethodException e false))) 
          node-classes))

(def
  ekeko-keywords-for-resolveable-ast-classes
  (map ekeko-keyword-for-class resolveable-node-classes))

(def
  ekeko-keywords-for-declaration-ast-classes
  (filter (fn [keyw] 
            (re-matches #".*Declaration" (str keyw)))
          ekeko-keywords-for-ast-classes))
  

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

(def 
  property-descriptors-per-node-class
  (memoize 
    (fn []
      (zipmap node-classes
              (map (fn [c] (nodeclass-property-descriptors c))
                   node-classes)))))

(def 
  owner-properties-per-node-class
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
    @ownerperclass))
  

;; Ekeko-specific properties
;; -------------------------

;TODO: switch to record as soon as core.logic no longer reifies records as maps
;(defrecord PropertyValueWrapper [owner property value])


;NOTE: not necessary to memoize:
;(identical? ((:modifiers (node-ekeko-properties node))) ((:modifiers (node-ekeko-properties node))))
;=> false;
;but:
;(= ((:modifiers (node-ekeko-properties node))) ((:modifiers (node-ekeko-properties node))))


(defn
  make-value
  [owner property value]
  {:type :Value
   :owner owner :property property :value value})

(defn
  value-unwrapped
  [value]
  (when
    (value? value)
    (:value value)))




(def 
  node-ekeko-properties-for-class
  (memoize
    (fn [^Class nc]
      (let [descriptors (nodeclass-property-descriptors nc)]
        (zipmap (map (fn [^StructuralPropertyDescriptor p] 
                       (keyword (property-descriptor-id p)))
                     descriptors)
                (map (fn [^StructuralPropertyDescriptor p]
                       (fn [n] 
                         (let [value (node-property-value n p)]
                           (if 
                             (ast? value)
                             value
                             (make-value n p value)))))
                     descriptors))))))

(defn
  node-ekeko-properties
  [node]
  (node-ekeko-properties-for-class (class node)))

(defprotocol 
  IHasOwner
  (owner [n-or-wrapper]))
          
(extend-protocol
  IHasOwner
  ASTNode 
  (owner [this] (.getParent ^ASTNode this))
  ;PropertyValueWrapper ;TODO: switch to record as soon as core.logic no longer reifies records as maps
  clojure.lang.IPersistentMap
  (owner [this] (:owner this)))

(defprotocol 
  IValueOfProperty
  ;PropertyValueWrapper ;TODO: switch to record as soon as core.logic no longer reifies records as maps  
  (owner-property [n-or-wrapper]))

(extend-protocol
  IValueOfProperty
  ASTNode 
  (owner-property [this] (.getLocationInParent ^ASTNode this))
  ;PropertyValueWrapper ;TODO: switch to record as soon as core.logic no longer reifies records as maps
  clojure.lang.IPersistentMap
  (owner-property [this] (:property this)))

(defprotocol 
  IAST
  (reifiers [this] 
    "Returns a map of keywords to reifier functions. The latter will return an Ekeko-specific child of the AST node."))

(extend 
  ASTNode
  IAST
  {:reifiers (fn [this] 
               (node-ekeko-properties-for-class (class this)))})
                          
(defn 
  node-propertyvalues
  [n]
  (map 
    (fn [retrievalf] (retrievalf n))
    (vals (reifiers n))))

(defn
  node-ancestors
  [^ASTNode n]
  (loop [ancestors []
         parent (.getParent n)]
    (if 
      parent
      (recur (conj ancestors parent)
             (.getParent parent))
      ancestors)))
     
(defn
  value-ancestors
  [v]
  (loop [ancestors []
         parent (owner v)]
    (if 
      parent
      (recur (conj ancestors parent)
             (owner parent))
      ancestors)))

(defn
  nodeorvalue-offspring
  [n]
  (loop [offspring []
         worklist [n]]
    (if 
      (empty? worklist)
      offspring
      (let [current 
            (first worklist)
            values  
            (cond (ast? current) (node-propertyvalues current)
                  (lstvalue? current) (:value current)
                  :default [])]
        (recur (concat offspring values)
               (concat (rest worklist) 
                       (filter (fn [value]
                                 (or (ast? value)
                                     (lstvalue? value)))
                               values)))))))                   
           


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



(defn
  register-callbacks
  []
  (set! (baristaui.views.queryResult.SOULLabelProvider/FN_ISWRAPPER) value?)
  (set! (baristaui.views.queryResult.SOULLabelProvider/FN_GETWRAPPEDVALUE) value-unwrapped))

(register-callbacks)
