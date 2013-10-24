(ns damp.ekeko.demo.csmrwcre2014
  (:require [clojure.core.logic :as logic])
  (:require [damp.ekeko.jdt.reification :as jdt])
  (:require [damp.ekeko.jdt.basic :as basic]))


;;Get Annotation

(defn annotation-fielddeclaration [?annotation ?field-decl]
  (logic/all
    (jdt/ast :Annotation ?annotation)
    (jdt/ast-parent ?annotation ?field-decl)
    (jdt/ast :FieldDeclaration ?field-decl)))


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

(defn simplename-simplename|same [?name-a ?name-b]
  (logic/fresh [?string]
    (jdt/simplename-stringname ?name-a ?string)
    (jdt/simplename-stringname ?name-b ?string)))

(defn type-annotation|correctlyannotated [?type ?annotation]
  (logic/fresh [?type-argument ?type-name ?annotation-type-literal ?annotation-type-name]
    (jdt/ast :ParameterizedType ?type)
    (jdt/child :typeArguments ?type ?type-argument)
    (jdt/has :name ?type-argument ?type-name)
    (entityannotation-typeliteral ?annotation ?annotation-type-literal)
    (typeliteral-name ?annotation-type-literal ?annotation-type-name)
    (simplename-simplename|same ?type-name ?annotation-type-name)))

(comment
(defn the-query [?field-declaration]
  (logic/fresh [?type-declaration ?field-type]
                       (annotation-named-entity ?annotation)
                       (annotation-field-declaration ?annotation ?field)
                       (damp.ekeko.jdt.basic/ast-encompassing-typedeclaration ?field ?type-declaration)
                       (is-type-declaration-astnode-subclass ?type-declaration)
                       (jdt/has :type ?field ?field-type)
                       (type-already-correctly-annotated ?field-type ?annotation))))
                   
