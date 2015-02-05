(ns damp.ekeko.jdt.astnode
  (:require [damp.util [interop :as interop]])
  (:import 
    [java.lang Class]
    [java.util List]
    [java.io Writer] 
    [java.lang.reflect Field]
    [damp.ekeko JavaProjectModel]
    [org.eclipse.jdt.core ICompilationUnit]
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
                              )
  (:import
    [org.eclipse.jdt.core JavaCore]
    [org.eclipse.jdt.core.dom 
     AST
     Expression Statement BodyDeclaration CompilationUnit ImportDeclaration
     Modifier
     Modifier$ModifierKeyword
     PrimitiveType
     PrimitiveType$Code  
     InfixExpression$Operator
     InfixExpression
     PrefixExpression$Operator
     PostfixExpression$Operator
     PostfixExpression
     PrefixExpression
     Assignment$Operator
     Assignment]))

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
  block?
  [node]
  (instance? Block node))

(defn
  statement?
  [node]
  (instance? Statement node))



;; JDT DOM classes
;; ---------------

(def 
  node-classes
  (let [lasttypeint 92]
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
(defn 
  nodeclass-property-descriptors 
  ([^Class cls jls]
    (clojure.lang.Reflector/invokeStaticMethod cls "propertyDescriptors" (to-array [jls])))
  ([^Class cls]
    (nodeclass-property-descriptors cls JavaProjectModel/JLS)))

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
           


;todo: should this not have been defined before?
(defn 
  node-poperty-value|reified
  [node property]
  ((get 
     (reifiers node)
     (ekeko-keyword-for-property-descriptor property))
    node))


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



;; Identifiers for values from projects and their persistence
;; ---------------------------------------------------------

;see also damp.ekeko.snippets.persistence!


;;(De)serializing of JDT objects

(def 
  ast-for-newlycreatednodes
  (AST/newAST JavaProjectModel/JLS))

(defn
  newnode 
  "Creates a new ASTNode in the given AST (or in one that is shared by all Ekeko instances)."
  ([ekekokeyword]
    (newnode ast-for-newlycreatednodes ekekokeyword))
  ([ast ekekokeyword]
    (let [nodeclass
          (class-for-ekeko-keyword ekekokeyword)]
      (.createInstance ^AST ast ^java.lang.Class nodeclass))))
    
(defn
  set-property!
  "Assigns property of ASTNode a given value."
  [^ASTNode node ^StructuralPropertyDescriptor propertydescriptor value]
  (.setStructuralProperty node propertydescriptor value))
  
(defn
  newnode-propertyvalues
  "Creates a new AST node with the given property values."
  [nodekeyword propertyvalues]
  (let [node (newnode nodekeyword)]
    (doseq [[property value] propertyvalues]
      (if
        (property-descriptor-list? property)
        (let [lst
              (node-property-value node property)]
          (.addAll ^List lst value))
        (set-property! node property value)))
    node))



(defn
  class-propertydescriptor-with-id
  "Returns property descriptor for given identifier and keyword of owner class."
  [ownerclasskeyword pdid]         
  (let [clazz
        (class-for-ekeko-keyword ownerclasskeyword)
        found 
        (some (fn [pd]
                (when (= pdid
                         (property-descriptor-id pd))
                  pd))
              
              (clojure.set/union
                (set (nodeclass-property-descriptors clazz AST/JLS4)) ;for older persisted ones
                (set (nodeclass-property-descriptors clazz))))]
    (if
      (nil? found)
      (throw (Exception. (str "When deserializing, could not find property descriptor: " ownerclasskeyword pdid)))
      found)))


;;duplicates ASTNode on given writer
(defmethod 
  clojure.core/print-dup 
  ASTNode
  [node w]
  (let [nodeclass
        (class node)
        nodeclasskeyword
        (ekeko-keyword-for-class nodeclass)]
    (let [propertyvalues
          (for [property (node-property-descriptors node)]
            (let [value (node-property-value node property)]
              [property
               (if
                 (property-descriptor-list? property)
                 (into [] value)
                 value)]))]
      (.write ^Writer w (str "#="
                             `(newnode-propertyvalues ~nodeclasskeyword ~propertyvalues))))))

;;duplicates StructuralPropertyDescriptor on given writer
(defmethod
  clojure.core/print-dup 
  StructuralPropertyDescriptor
  [pd w]
  (let [id 
        (property-descriptor-id pd)
        ownerclass
        (property-descriptor-owner-node-class pd)
        ownerclass-keyword
        (ekeko-keyword-for-class ownerclass)
        ]
    (.write ^Writer w (str  "#=" `(class-propertydescriptor-with-id ~ownerclass-keyword ~id)))))


;;duplicates annoying non-ASTNode subtypes among JDT values
(defn
  modifierkeyword-for-flagvalue
  [flagvalue]
  (Modifier$ModifierKeyword/fromFlagValue flagvalue))


(defmethod 
  clojure.core/print-dup 
  Modifier$ModifierKeyword
  [node w]
  (let [flagvlue (.toFlagValue ^Modifier$ModifierKeyword node)]
    (.write ^Writer w (str  "#=" `(modifierkeyword-for-flagvalue ~flagvlue)))))


(defn
  primitivetypecode-for-string
  [codestr]
  (PrimitiveType/toCode codestr))

(defmethod 
  clojure.core/print-dup 
  PrimitiveType$Code  
  [node w]
  (let [codestr (.toString ^PrimitiveType$Code node)]
    (.write ^Writer w (str  "#=" `(primitivetypecode-for-string ~codestr)))))

(defn
  infixexpressionoperator-for-string
  [opstr]
  (InfixExpression$Operator/toOperator opstr))


(defmethod 
  clojure.core/print-dup 
  InfixExpression$Operator
  [node w]
  (let [codestr (.toString ^InfixExpression$Operator node)]
    (.write ^Writer w (str  "#=" `(infixexpressionoperator-for-string ~codestr)))))

(defn
  assignmentoperator-for-string
  [opstring]
  (Assignment$Operator/toOperator opstring))

(defmethod 
  clojure.core/print-dup 
  Assignment$Operator
  [node w]
  (let [codestr (.toString ^Assignment$Operator node)]
    (.write ^Writer w (str  "#=" `(assignmentoperator-for-string ~codestr)))))


(defn
  prefixexpressionoperator-for-string
  [opstring]
  (PrefixExpression$Operator/toOperator opstring))

(defmethod 
  clojure.core/print-dup 
  PrefixExpression$Operator
  [node w]
  (let [codestr (.toString ^PrefixExpression$Operator node)]
    (.write ^Writer w (str  "#=" `(prefixexpressionoperator-for-string ~codestr)))))


(defn
  postfixexpressionoperator-for-string
  [opstring]
  (PostfixExpression$Operator/toOperator opstring))

(defmethod 
  clojure.core/print-dup 
  PostfixExpression$Operator
  [node w]
  (let [codestr (.toString ^PostfixExpression$Operator node)]
    (.write ^Writer w (str  "#=" `(postfixexpressionoperator-for-string ~codestr)))))




;; Unique identifiers for JDT objects in a particular project (won't survice project renames though)


(defrecord
  ProjectRootIdentifier [icuhandle])

(defn
  make-root-identifier|project
  [icuhandlestr]
  (ProjectRootIdentifier. icuhandlestr))

(defmethod 
  clojure.core/print-dup 
  ProjectRootIdentifier
  [identifier w]
  (.write ^Writer w (str  "#=" `(make-root-identifier|project))))

(defn
  root-identifier|project
  [^CompilationUnit cu]
  (when-let [icu (.getJavaElement cu)]
    (let [handlestr (.getHandleIdentifier icu)]
      handlestr)))

(defrecord
  RelativePropertyValueIdentifier
  [ownerid
   property])

(defn
  make-property-value-identifier
  [ownerid property]
  (RelativePropertyValueIdentifier. ownerid property))

(defmethod 
  clojure.core/print-dup 
  RelativePropertyValueIdentifier
  [identifier w]
  (let [ownerid (:ownerid identifier)
        property (:property identifier)]
  (.write ^Writer w (str  "#=" `(make-property-value-identifier ~ownerid ~property)))))


(defrecord
  RelativeListElementIdentifier
  [listid
   index
   ])

(defn
  make-list-element-identifier
  [listid index]
  (RelativeListElementIdentifier. listid index))

(defmethod 
  clojure.core/print-dup 
  RelativeListElementIdentifier
  [identifier w]
  (let [listid (:listid identifier)
        index (:index identifier)]
  (.write ^Writer w (str  "#=" `(make-list-element-identifier ~listid ~index)))))


(defn
  jdt-parse-icu 
  "Parses the given ICompilationUnit into a CompilationUnit ASTNode.
   Note that the node will differ from the ones being queried by Ekeko."
  [^ICompilationUnit icu]
  (JavaProjectModel/parse icu nil))

(defprotocol 
  IIdentifiesProjectValue
  (corresponding-project-value [identifier]
    "Returns the JDT value corresponding to the given unique identifier (produced before by project-value-indentifier),
     if it still exists.  Assumes the source project has not been renamed in the mean time."))

(extend-protocol IIdentifiesProjectValue
  ProjectRootIdentifier
  (corresponding-project-value [id]
    (let [^String handle (:icuhandle id)]
      (if-let [icu (JavaCore/create handle)]
        (jdt-parse-icu icu) ;these ASTs are different from those queried by Ekeko
        (throw (Exception. (str "While looking for value in project, could not find its file using handle: " handle))))))
  RelativePropertyValueIdentifier
  (corresponding-project-value [id]
    (let [ownerid (:ownerid id)
          property (:property id)
          owner (corresponding-project-value ownerid)]
        (node-poperty-value|reified owner property)))
  RelativeListElementIdentifier
  (corresponding-project-value [id]
    (let [listid (:listid id)
          idx (:index id)
          lst (corresponding-project-value listid)
          lst-raw (value-unwrapped lst)]
        (.get ^List lst-raw idx))))



(defn
  project-value-identifier
  "Returns a unique identifier for the given JDT value in the project it originates from."
  [value]
  (let [owner (owner value) ;owner of list = node, owner of list element = node (never list)
        property (owner-property value)]
    (cond 
      ;root
      (instance? org.eclipse.jdt.core.dom.CompilationUnit value)
      (make-root-identifier|project (root-identifier|project value))
    
      ;lists (keep before next clause, do not merge with before-last clause)
      (lstvalue? value)
      (make-property-value-identifier 
        (project-value-identifier owner)
        property)
    
      ;list members
      (property-descriptor-list? property)
      (let [lst 
            (node-poperty-value|reified owner property)
            lst-raw (value-unwrapped lst)]
        (make-list-element-identifier 
          (project-value-identifier lst)
          (.indexOf ^List lst-raw value)))
    
      ;non-list members
      (or 
        (ast? value)
        (nilvalue? value)
        (primitivevalue? value))
      (make-property-value-identifier
        (project-value-identifier owner)
        property)
    
      :else
      (throw (Exception. (str "Unknown project value to create identifier for:" value))))))

(defn
  project-tuple-identifier
  "Returns a seq of identifiers for the given seq of JDT values."
  [tuple]
  (map project-value-identifier tuple))


(defn
  astnode-as-persistent-string
  "Serializes given JDT value to a string."
  [node]
  (binding [*print-dup* true]
    (pr-str node)))

(defn
  astnode-from-persistent-string
  "Deserializes  given JDT value from a string."
  [string]
  (binding [*read-eval* true]
    (read-string string)))




;; Misc
;; ----

;put below because it relies on protocol functions
;declaring them ahead causes warning:
;Warning: protocol #'damp.ekeko.jdt.astnode/IValueOfProperty is overwriting function owner-property
(defn
  valuelistmember? 
  "Checks whether value is a member of a list."
  [snippet-val]
  (and 
    (not (nil? snippet-val))
    (not (lstvalue? snippet-val))
    (when-let [ownerproperty (owner-property snippet-val)]
      (property-descriptor-list? ownerproperty))))



(comment

  ;;Example uses of project-value-identifier and corresponding-project-value
  
  (def m (first (first (damp.ekeko/ekeko [?m] (damp.ekeko.jdt.ast/ast :MethodDeclaration ?m)))))
  (def mid (project-value-identifier m))
  (def equivalenttom (corresponding-project-value mid))
  (= (str m) (str equivalenttom))
    
  
  (reduce (fn [sofar t] 
             (let [exp (first t)
                   expid (project-value-identifier exp)
                   equivalent (corresponding-project-value expid)]
               (and sofar (= (str exp) (str equivalent)))))
           (damp.ekeko/ekeko [?e ?key] (damp.ekeko.jdt.ast/ast ?key ?e)))
  
  
  
  (def serialized
    (binding [*print-dup* true]
      (pr-str m)))
  
  (def 
    deserialized
    (binding [*read-eval* true] 
      (read-string serialized)))
  


  
  
  )




(defn
  register-callbacks
  []
  (set! (baristaui.views.queryResult.SOULLabelProvider/FN_ISWRAPPER) value?)
  (set! (baristaui.views.queryResult.SOULLabelProvider/FN_GETWRAPPEDVALUE) value-unwrapped))

(register-callbacks)
