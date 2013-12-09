(ns damp.ekeko.demo.csmrwcre2014
  (:require [clojure.core.logic :as logic])
  (:refer-clojure :exclude [== type])
  (:use clojure.core.logic)
  (:use damp.ekeko)
  (:use [damp.ekeko logic])
  (:use [damp.ekeko.soot soot])
  (:use [damp.ekeko.jdt ast astnode structure aststructure soot convenience markers rewrites])
  (:import [org.eclipse.ui.IMarkerResolution])
  (:import [damp.ekeko.EkekoProblemFixer]))


;;The archive containing the source code can be found here: http://soft.vub.ac.be/~resteven/csmrwcrecase.zip
;;Ensure it has the Ekeko Nature by rightclicking on the project, configure, include in Ekeko Queries

;;Start a REPL by selecting Ekeko->start nREPL from the menu
;;Connect to that REPL by selecting Window->Connect to REPL from the menu

;;Open this file in the  Clojure editor (by double clicking the file in the Eclipse package explorer)
;;Rightclick within the editor and select: Clojure->Load File in REPL
;;In the same menu select Switch REPL to File's namespace

;;You can run the code by just writing (demo)
;;This will mark all the incorrect nodes with an annotation marker
;;You can solve a marker by calling (marker-quick-fix (first @markers/markers))
;;Or by clicking on the warning popup and use the quick fix


(defn typedeclaration|inhierarchy
  [?type-decl]
  (fresh [?astnode]
    (typedeclaration-name|qualified|string ?astnode "be.ac.chaq.model.ast.java.ASTNode")
    (typedeclaration-typedeclaration|super ?type-decl ?astnode)))

(defn annotation|namedentityproperty [?annotation]
  (all
    (annotation-name|qualified|string ?annotation "be.ac.chaq.model.entity.EntityProperty")))

(defn annotation|ep-typeliteral [?annotation ?type-literal]
  (fresh [?member-pair ?name]
    (annotation|namedentityproperty ?annotation)
    (child :values ?annotation ?member-pair)
    (has :name ?member-pair ?name)
    (name|simple-string ?name "value")
    (has :value ?member-pair ?type-literal)))
  

(defn 
  type-annotation|correct 
  [?asttype ?annotation]
  (fresh [?targ ?type-name ?annotypelit  ?annotype]
         (ast :ParameterizedType ?asttype)
         (child :typeArguments ?asttype ?targ)
         (annotation|ep-typeliteral ?annotation ?annotypelit)
         (typeliteral-type ?annotypelit ?annotype)
         (fresh [?typekind]
                (ast|type-type ?typekind ?targ ?annotype))))

(defn field-declaration|incorrect [?field-declaration]
  (fresh [?type-declaration ?field-type ?annotation]
    (annotation|namedentityproperty ?annotation)
    (fielddeclaration-annotation ?field-declaration ?annotation)
    (ast-typedeclaration|encompassing ?field-declaration ?type-declaration)
    (typedeclaration|inhierarchy ?type-declaration)
    (has :type ?field-declaration ?field-type)
    (fails
      (type-annotation|correct ?field-type ?annotation))))

;;Install Marker
(defn 
  add-annotation-problem-marker [astnode]
  (add-problem-marker astnode "Type Parameter is missing" "annotation"))

;;Rewrite Magic

(defn fielddeclaration-getinfo [typedeclaration]
  (first
    (ekeko [?annotation ?type]
           (fresh [?typeliteral]
                  (child :modifiers typedeclaration ?annotation)
                  (annotation|namedentityproperty ?annotation)
                  (annotation|ep-typeliteral ?annotation ?typeliteral)
                  (has :type ?typeliteral ?type)))))
   

(defn node-property [node keyword]
  (node-property-value
    node
    (node-property-descriptor-for-ekeko-keyword node keyword)))


;;Marker Fixing

(defn marker-quick-fix [marker]
  (let [fielddecl (ekekomarker-astnode marker)
        type (node-property fielddecl :type)
        [anno anno-type] (fielddeclaration-getinfo fielddecl)
        ast (.getAST type)
        type-copy (copy-astnode type)
        anno-type-copy (copy-astnode anno-type)
        new-node (create-parameterized-type type-copy)]
    (change-property-node fielddecl :type new-node)
    (add-node-cu (.getRoot fielddecl) new-node :typeArguments anno-type-copy 0)
    (apply-and-reset-rewrites)
    (reset-and-delete-marker marker)))

(defn annotation-fixer []
  (create-quick-fix "Add Type Parameter from Annotation" marker-quick-fix))

(defn install-fixer []
  (damp.ekeko.EkekoProblemFixer/installNewResolution "annotation" (annotation-fixer)))


;;Demo
(defn 
  demo
  []
  (map (comp add-annotation-problem-marker first) 
       (ekeko [?ast] (field-declaration|incorrect ?ast))))


(install-fixer)
