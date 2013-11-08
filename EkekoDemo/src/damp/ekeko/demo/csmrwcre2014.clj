(ns damp.ekeko.demo.csmrwcre2014
  (:require [clojure.core.logic :as logic])
  (:require [damp.ekeko.jdt
             [reification :as jdt]
             [basic :as basic]
             [astnode :as astnode]
             [markers :as markers]
             [rewrites :as rewrites]])
  (:import [org.eclipse.ui.IMarkerResolution])
  (:import [damp.ekeko.EkekoProblemFixer]))


;;The archive containing the source code can be found here: http://soft.vub.ac.be/~resteven/csmrwcrecase.zip
;;Ensure it has the Ekeko Nature by rightclicking on the project, configure, include in Ekeko Queries

;;Start a REPL by selecting Ekeko->start nREPL from the menu
;;Connect to that REPL by selecting Window->Connect to REPL from the menu

;;Rightclick on this file and select: Clojure->Load File in REPL
;;In the same menu select Switch REPL to File's namespace

;;You can run the code by just writing (demo)
;;This will mark all the incorrect nodes with an annotation marker
;;You can solve a marker by calling (marker-quick-fix (first @markers/markers))
;;Or by clicking on the warning popup and use the quick fix


(defn typedeclaration|inhierarchy [?type-decl]
  (logic/fresh [?astnode]
    (jdt/ast :TypeDeclaration ?astnode)
    (jdt/typedeclaration-qualifiedname ?astnode "be.ac.chaq.model.ast.java.ASTNode")
    (jdt/typedeclaration-typedeclaration|super ?type-decl ?astnode)))

(defn annotation|namedentityproperty [?annotation]
  (logic/all
    (jdt/annotation-qualifiedname ?annotation "be.ac.chaq.model.entity.EntityProperty")))


(defn annotation|ep-typeliteral [?annotation ?type-literal]
  (logic/fresh [?member-pair ?name]
    (annotation|namedentityproperty ?annotation)
    (jdt/child :values ?annotation ?member-pair)
    (jdt/has :name ?member-pair ?name)
    (jdt/simplename-stringname ?name "value")
    (jdt/has :value ?member-pair ?type-literal)))
  

(defn type-annotation|correct [?type ?annotation]
  (logic/fresh [?type-argument ?type-name ?annotation-type-literal  ?annotation-type]
    (jdt/ast :ParameterizedType ?type)
    (jdt/child :typeArguments ?type ?type-argument)
    (annotation|ep-typeliteral ?annotation ?annotation-type-literal)
    (jdt/typeliteral-type ?annotation-type-literal ?annotation-type)
    (jdt/type-type|same ?type-argument ?annotation-type)))


(defn field-declaration|incorrect [?field-declaration]
  (logic/fresh [?type-declaration ?field-type ?annotation]
    (annotation|namedentityproperty ?annotation)
    (jdt/annotation-fielddeclaration ?annotation ?field-declaration)
    (basic/ast-encompassing-typedeclaration ?field-declaration ?type-declaration)
    (typedeclaration|inhierarchy ?type-declaration)
    (jdt/has :type ?field-declaration ?field-type)
    (damp.ekeko.logic/fails
      (type-annotation|correct ?field-type ?annotation))))

                


;;Install Marker
(defn add-annotation-problem-marker [astnode]
  (markers/add-problem-marker astnode "Type Parameter is missing" "annotation"))

;;Rewrite Magic

(defn fielddeclaration-getinfo [typedeclaration]
  (first
    (logic/run* [?annotation ?type]
                (logic/fresh [?typeliteral]
                (jdt/child :modifiers typedeclaration ?annotation)
                (annotation|namedentityproperty ?annotation)
                (annotation|ep-typeliteral ?annotation ?typeliteral)
                (jdt/has :type ?typeliteral ?type)))))
   

(defn node-property [node keyword]
  (astnode/node-property-value
    node
    (astnode/node-property-descriptor-for-ekeko-keyword node keyword)))


;;Marker Fixing

(defn marker-quick-fix [marker]
  (let [fielddecl (markers/ekekomarker-astnode marker)
        type (node-property fielddecl :type)
        [anno anno-type] (fielddeclaration-getinfo fielddecl)
        ast (.getAST type)
        type-copy (rewrites/copy-astnode type)
        anno-type-copy (rewrites/copy-astnode anno-type)
        new-node (rewrites/create-parameterized-type type-copy)]
    (rewrites/change-property-node fielddecl :type new-node)
    (rewrites/add-node-cu (.getRoot fielddecl) new-node :typeArguments anno-type-copy 0)
    (rewrites/apply-and-reset-rewrites)
    (markers/reset-and-delete-marker marker)))

(defn annotation-fixer []
  (markers/create-quick-fix "Add Type Parameter from Annotation" marker-quick-fix))

(defn install-fixer []
  (damp.ekeko.EkekoProblemFixer/installNewResolution "annotation" (annotation-fixer)))


;;Demo
(defn demo []
  (let [wrong-nodes (logic/run* [?ast]
                                (field-declaration|incorrect ?ast))]
    (map add-annotation-problem-marker wrong-nodes)))


(install-fixer)
