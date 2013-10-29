(ns damp.ekeko.jdt.rewrites
  (:import [damp.ekeko.EkekoProblemFixer])
  (:import 
    [damp.ekeko JavaProjectModel]
    [org.eclipse.jface.text Document]
    [org.eclipse.text.edits TextEdit]
    [org.eclipse.jdt.core ICompilationUnit IJavaProject]
    [org.eclipse.jdt.core.dom BodyDeclaration Expression Statement ASTNode ASTParser AST CompilationUnit]
    [org.eclipse.jdt.core.dom.rewrite ASTRewrite])
  (:require [damp.ekeko.jdt
             [astnode :as astnode]]))

(defn make-rewrite-for-cu [cu]
  (ASTRewrite/create (.getAST cu)))

(def current-rewrites (atom {}))

(defn reset-rewrites! []
  (reset! current-rewrites {}))

(defn current-rewrite-for-cu [cu]
  (if-let [rw (get @current-rewrites cu)]
    rw
    (let [nrw (make-rewrite-for-cu cu)]
      (swap! current-rewrites assoc cu nrw)
      nrw))) 

(defn apply-rewrite-to-node [rw node]
  (JavaProjectModel/applyRewriteToNode rw node))

(defn apply-rewrites []
  (doseq [[cu rw] @current-rewrites]
    (apply-rewrite-to-node rw cu)))

(defn apply-and-reset-rewrites []
  (do
    (apply-rewrites)
    (reset-rewrites!)))


;;Operations
(defn remove-node 
  "Remove node."
  [node]
  (let [cu (.getRoot node)
        rewrite (current-rewrite-for-cu cu)
        list-rewrite (.getListRewrite rewrite (.getParent node) (.getLocationInParent node))]
    (.remove list-rewrite node nil)))

(defn add-node 
  "Add newnode to propertyList of the given parent at idx position."
  [parent propertykey newnode idx]
  (let [cu (.getRoot parent)
        rewrite (current-rewrite-for-cu cu)
        property (astnode/node-property-descriptor-for-ekeko-keyword parent propertykey) 
        list-rewrite (.getListRewrite rewrite parent property)
        index (if (instance? java.lang.String idx)
               (Integer/parseInt idx)
               idx)] 
    (.insertAt list-rewrite newnode index nil)))

(defn add-node-cu
  "Add newnode to propertyList of the given parent at idx position."
  [cu parent propertykey newnode idx]
  (let [rewrite (current-rewrite-for-cu cu)
        property (astnode/node-property-descriptor-for-ekeko-keyword parent propertykey) 
        list-rewrite (.getListRewrite rewrite parent property)
        index (if (instance? java.lang.String idx)
               (Integer/parseInt idx)
               idx)] 
    (.insertAt list-rewrite newnode index nil)))


(defn replace-node 
  "Replace node with newnode."
  [node newnode]
  (let [cu (.getRoot node)
        rewrite (current-rewrite-for-cu cu)] 
    (.replace rewrite node newnode nil)))


(defn change-property-node
  "Change property node."
  [node propertykey value]
  (let [cu (.getRoot node)
        rewrite (current-rewrite-for-cu cu)
        property (astnode/node-property-descriptor-for-ekeko-keyword node propertykey)] 
    (.set rewrite node property value nil)))

(defn copy-astnode [astnode]
  (ASTNode/copySubtree (.getAST astnode) astnode))

(defn create-parameterized-type [type]
  (.newParameterizedType (.getAST type) type))
