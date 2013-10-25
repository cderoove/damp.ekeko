(ns damp.ekeko.demo.csmrwcre2014
  (:require [clojure.core.logic :as logic])
  (:require [damp.ekeko.jdt
             [reification :as jdt]
             [basic :as basic]
             [astnode :as astnode]])
  (:import [org.eclipse.ui.IMarkerResolution])
  (:import [damp.ekeko.EkekoProblemFixer])
  (:import 
    [damp.ekeko JavaProjectModel]
    [org.eclipse.jface.text Document]
    [org.eclipse.text.edits TextEdit]
    [org.eclipse.jdt.core ICompilationUnit IJavaProject]
    [org.eclipse.jdt.core.dom BodyDeclaration Expression Statement ASTNode ASTParser AST CompilationUnit]
    [org.eclipse.jdt.core.dom.rewrite ASTRewrite] ))




(defn typedeclaration|astnodesubclass [?type-decl]
  (logic/fresh [?astnode]
    (jdt/ast :TypeDeclaration ?astnode)
    (jdt/typedeclaration-qualifiedname ?astnode "be.ac.chaq.model.ast.java.ASTNode")
    (jdt/typedeclaration-typedeclaration|super ?type-decl ?astnode)))

(defn annotation|namedentityproperty [?annotation]
  (logic/all
    (jdt/annotation-qualifiedname ?annotation "be.ac.chaq.model.entity.EntityProperty")))


(defn entityannotation-typeliteral [?annotation ?type-literal]
  (logic/fresh [?member-pair ?name]
    (annotation|namedentityproperty ?annotation)
    (jdt/child :values ?annotation ?member-pair)
    (jdt/has :name ?member-pair ?name)
    (jdt/simplename-stringname ?name "value")
    (jdt/has :value ?member-pair ?type-literal)))

(defn typeliteral-name [?type-literal ?name]
  (logic/fresh [?type]
    (jdt/has :type ?type-literal ?type)
    (jdt/has :name ?type ?name)))

(defn type-annotation|correctlyannotated [?type ?annotation]
  (logic/fresh [?type-argument ?type-name ?annotation-type-literal ?annotation-type-name]
    (jdt/ast :ParameterizedType ?type)
    (jdt/child :typeArguments ?type ?type-argument)
    (jdt/has :name ?type-argument ?type-name)
    (entityannotation-typeliteral ?annotation ?annotation-type-literal)
    (typeliteral-name ?annotation-type-literal ?annotation-type-name)
    (jdt/simplename-simplename|same ?type-name ?annotation-type-name)))

(defn field-declaration|incorrect [?field-declaration]
  (logic/fresh [?type-declaration ?field-type ?annotation]
    (annotation|namedentityproperty ?annotation)
    (jdt/annotation-fielddeclaration ?annotation ?field-declaration)
    (damp.ekeko.jdt.basic/ast-encompassing-typedeclaration ?field-declaration ?type-declaration)
    (typedeclaration|astnodesubclass ?type-declaration)
    (jdt/has :type ?field-declaration ?field-type)
    (damp.ekeko.logic/fails
      (type-annotation|correctlyannotated ?field-type ?annotation))))

                


;;Markers
(def markers (atom '()))

(defn add-problem-marker [astnode message kind]
  (let [compunit (.getRoot astnode)
        start-pos (.getStartPosition astnode)
        line-no (.getLineNumber compunit start-pos)
        resource (.getCorrespondingResource (.getJavaElement compunit))
        marker (.createMarker resource "damp.ekeko.plugin.ekekoproblemmarker")]
    (.setAttribute marker org.eclipse.core.resources.IMarker/LINE_NUMBER (int line-no))
    (.setAttribute marker org.eclipse.core.resources.IMarker/CHAR_START (int start-pos))
    (.setAttribute marker org.eclipse.core.resources.IMarker/CHAR_END (int (+ start-pos (.getLength astnode))))
    (.setAttribute marker 
      org.eclipse.core.resources.IMarker/SEVERITY
      (int org.eclipse.core.resources.IMarker/SEVERITY_WARNING))
    (.setAttribute marker 
      org.eclipse.core.resources.IMarker/MESSAGE 
      message)
    (.setAttribute marker "ekekoKind" kind)
    (.setAttribute marker "astnode" astnode) ;;pray this works
    (swap! markers conj marker)
    marker))


(defn add-annotation-problem-marker [astnode]
  (add-problem-marker astnode "Generic Type is missing" "annotation"))



(defn reset-and-delete-markers []
  (do
    (map #(.delete %) @markers)
    (reset! markers '())))


(defn ekekomarker-astnode [marker]
  (.getAttribute marker "astnode"))

    

;;Rewrites
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

;;Rewrite Magic

(defn fielddeclaration-getinfo [typedeclaration]
  (first
    (logic/run* [?annotation ?type]
                (logic/fresh [?typeliteral]
                (jdt/child :modifiers typedeclaration ?annotation)
                (annotation|namedentityproperty ?annotation)
                (entityannotation-typeliteral ?annotation ?typeliteral)
                (jdt/has :type ?typeliteral ?type)))))
   

(defn node-property [node keyword]
  (astnode/node-property-value
    node
    (astnode/node-property-descriptor-for-ekeko-keyword node keyword)))


;;Marker Fixing
(defn marker-quick-fix [marker]
  (let [fielddecl (ekekomarker-astnode marker)
        type (node-property fielddecl :type)
        [anno anno-type] (fielddeclaration-getinfo fielddecl)
        ast (.getAST type)
        type-copy (ASTNode/copySubtree ast type)
        anno-type-copy (ASTNode/copySubtree ast anno-type)
        new-node (.newParameterizedType ast type-copy)]
    (do
      (change-property-node fielddecl :type new-node)
      (add-node-cu (.getRoot fielddecl) new-node :typeArguments anno-type-copy 0)
      (apply-and-reset-rewrites)
      (reset-and-delete-markers)))


(defn annotation-fixer []
  (reify
    org.eclipse.ui.IMarkerResolution
    (getLabel [this] "Add Generic from Annotation")
    (run [this marker] 
      (marker-quick-fix marker))))

(defn install-fixer []
  (damp.ekeko.EkekoProblemFixer/installNewResolution "annotation" (annotation-fixer)))

(defn demo []
  (let [wrong-nodes (logic/run* [?ast]
                                (field-declaration|incorrect ?ast))]
    (map add-annotation-problem-marker wrong-nodes)))


(install-fixer)
