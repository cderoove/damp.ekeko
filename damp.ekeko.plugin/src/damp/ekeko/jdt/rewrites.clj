(ns 
  damp.ekeko.jdt.rewrites
  ^{:doc "Functional interface to the Eclipse rewriting API."
    :author "Coen De Roover, Reinout Stevens, Siltvani"}
(:import 
  [damp.ekeko JavaProjectModel]
  [org.eclipse.jface.text Document]
  [org.eclipse.text.edits TextEdit]
  [org.eclipse.jdt.core ICompilationUnit IJavaProject]
  [org.eclipse.jdt.core.dom BodyDeclaration Expression Statement ASTNode ASTParser AST CompilationUnit]
  [org.eclipse.jdt.core.dom.rewrite ASTRewrite])
  (:require [damp.ekeko.jdt
             [astnode :as astnode]]))

(defn-
  make-rewrite-for-cu
  [cu]
  (ASTRewrite/create (.getAST cu)))

(def current-rewrites (atom {}))

(defn 
  reset-rewrites!
  []
  (reset! current-rewrites {}))

(defn
 current-rewrite-for-cu 
 [cu]
 (if-let [rw (get @current-rewrites cu)]
  rw
  (let [nrw (make-rewrite-for-cu cu)]
   (swap! current-rewrites assoc cu nrw)
   nrw))) 

(defn- 
  apply-rewrite-to-node
  [rw node]
  (JavaProjectModel/applyRewriteToNode rw node))

(defn- 
  apply-rewrites
  []
  (doseq [[cu rw] @current-rewrites]
    (apply-rewrite-to-node rw cu)))

(defn 
  apply-and-reset-rewrites
  []
  (do
    (apply-rewrites)
    (reset-rewrites!)))


(declare copy-astnode)

(defn-
  compatible
  [ast astnodeorvalue]
  (cond 
    (instance? java.util.List astnodeorvalue)
    (map (partial compatible ast) astnodeorvalue)
    (astnode/ast? astnodeorvalue)
    (if
      (= ast (.getAST astnodeorvalue))
      astnodeorvalue
      (copy-astnode ast astnodeorvalue))
    :default
    astnodeorvalue))




;;Operations
(defn 
  remove-node 
  "Remove node."
  ([rewrite node]
    (let [list-rewrite (.getListRewrite rewrite (.getParent node) (.getLocationInParent node))]
      (.remove list-rewrite node nil)))
  ([node]
    (let [cu (.getRoot node)
          rewrite (current-rewrite-for-cu cu)]
      (remove-node rewrite node))))
          

(defn 
  add-node 
  "Add newnode (or clone of newnode when ASTs are incompatible) to propertyList of the given parent at idx position."
  ([parent propertykey newnode idx] 
    (let [cu (.getRoot parent)
          rewrite (current-rewrite-for-cu cu)
         ]
      (add-node rewrite parent propertykey newnode idx)))
  ([rewrite parent propertykey newnode idx]  
    (let [value 
          (compatible (.getAST rewrite) newnode)
          property 
          (astnode/node-property-descriptor-for-ekeko-keyword parent propertykey) 
          list-rewrite 
          (.getListRewrite rewrite parent property)
          index 
          (if (instance? java.lang.String idx)
            (Integer/parseInt idx)
            idx)] 
      (.insertAt list-rewrite value index nil))))

(defn
  add-element
  "Add newnode to the given list at position idx."
  ([rewrite lst newnode idx]
    (let [owner (astnode/owner lst)
          ownerproperty (astnode/owner-property lst)]
      (add-node rewrite owner (astnode/ekeko-keyword-for-property-descriptor ownerproperty) newnode idx)))
  ([lst newnode idx]
    (let [owner (astnode/owner lst)
          ownerproperty (astnode/owner-property lst)]
      (add-node owner (astnode/ekeko-keyword-for-property-descriptor ownerproperty) newnode idx))))
        
  
(defn 
  replace-node 
  "Replace node with newnode (if ASTs are compatible) or clone of newnode (when ASTs are incompatible). "
  ([rewrite node newnode]
    (.replace rewrite node 
      (compatible (.getAST rewrite) newnode)
      nil))
  ([node newnode]
    (let [cu (.getRoot node)
          rewrite (current-rewrite-for-cu cu)]
      (replace-node rewrite node newnode))))


(def 
  ast-for-newlycreatednodes
  (AST/newAST JavaProjectModel/JLS))

;todo: fix duplication


(defn-
  assignproperties
  [instance  propertykey2value]
  (doseq [[key value] propertykey2value]
      (let [desc (astnode/node-property-descriptor-for-ekeko-keyword instance key)
            val (compatible (.getAST instance) value)]
        (if 
          (astnode/property-descriptor-list? desc)
          (let [lstval (.getStructuralProperty instance desc)]
            (.addAll lstval val))
          (.setStructuralProperty instance desc val)))))


(defn
  newast-for-rewrite
  "Creates a new node of the given ekeko keyword (and optional property keyword to value pairs)."
  [rewrite key & {:as propertykey2value}]
  (let [clazz (astnode/class-for-ekeko-keyword key)
        instance (.createInstance (.getAST rewrite) clazz)]
    (assignproperties instance propertykey2value)
    instance))

(defn 
  newast
  "Creates a new node of the given ekeko keyword (and optional property keyword to value pairs)."
  [key & {:as propertykey2value}]
  (let [clazz (astnode/class-for-ekeko-keyword key)
        instance (.createInstance ast-for-newlycreatednodes clazz)]
      (assignproperties instance propertykey2value)
      instance))
    
    
(defn 
  change-property-node
  "Change property of node."
  ([rewrite node propertykey value]
    (let [property (astnode/node-property-descriptor-for-ekeko-keyword node propertykey)] 
      (.set rewrite node property value nil)))
  ([node propertykey value]
    (let [cu (.getRoot node)
          rewrite (current-rewrite-for-cu cu)]
      (change-property-node rewrite node propertykey value))))

(defn
  replace-value
  "Replaces a non-ASTNode primitive value by the given value."
  ([value newvalue]
    (let [owner (astnode/owner value)
          cu (.getRoot owner)
          rewrite (current-rewrite-for-cu cu)]
      (replace-value rewrite value newvalue)))
  ([rewrite value newvalue]
    (let [owner (astnode/owner value)
          ownerproperty (astnode/owner-property value)
          ekekopropertykeyword (astnode/ekeko-keyword-for-property-descriptor ownerproperty)]
      (change-property-node rewrite owner ekekopropertykeyword newvalue))))


(defn 
  copy-astnode 
  ([ast astnode]
    (ASTNode/copySubtree ast astnode))
  ([astnode]
    (copy-astnode (.getAST astnode) astnode)))

(defn 
  create-parameterized-type [type]
  (.newParameterizedType (.getAST type) type))

