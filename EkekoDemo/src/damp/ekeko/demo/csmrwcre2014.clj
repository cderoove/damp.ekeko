(ns damp.ekeko.demo.csmrwcre2014
  (:require [clojure.core.logic :as logic])
  (:require [damp.ekeko.jdt.reification :as jdt])
  (:require [damp.ekeko.jdt.basic :as basic]))


;;Get Annotation

(defn annotation-field-declaration [annotation field-decl]
  (logic/all
    (jdt/ast :Annotation annotation)
    (jdt/ast-parent annotation field-decl)
    (jdt/ast :FieldDeclaration field-decl)))

(defn type-declaration-superclass 
  "this is art"
  [?type-decl ?superclass]
  (defn superclass-helper [class-a class-b]
    (if-not (nil? class-a)
      (or (= (.getFullyQualifiedName (.getName class-a))
             (.getFullyQualifiedName (.getName class-b)))
          (if (= (.getNodeType class-a) org.eclipse.jdt.core.dom.ASTNode/TYPE_DECLARATION)
            (recur (.getSuperclassType class-a) class-b)
            false))
      false))
  (logic/all
    (jdt/ast :TypeDeclaration ?type-decl)
    (jdt/ast :TypeDeclaration ?superclass)
    (logic/!= ?type-decl ?superclass)
    (logic/project [?type-decl ?superclass]
                   (logic/== true
                             (superclass-helper ?type-decl ?superclass)))))





(defn is-type-declaration-astnode-subclass [?type-decl]
  (logic/fresh [?super]
    (jdt/ast :TypeDeclaration ?type-decl)
    (type-declaration-superclass ?type-decl ?super)
    (jdt/type-declaration-string-name ?super "ASTNode")))

(defn annotation-named-entity [?annotation]
  (logic/fresh [?name]
    (jdt/annotation-is-named ?annotation ?name)
    (jdt/name-fully-qualified-name ?name "EntityProperty")))


(defn entity-annotation-type-literal [?annotation ?type-literal]
  (logic/fresh [?member-pair ?name]
    (annotation-named-entity ?annotation)
    (jdt/child :values ?annotation ?member-pair)
    (jdt/has :name ?member-pair ?name)
    (jdt/name-fully-qualified-name ?name "value")
    (jdt/has :value ?member-pair ?type-literal)))

(defn type-literal-name [?type-literal ?name]
  (logic/fresh [?type]
    (jdt/has :type ?type-literal ?type)
    (jdt/has :name ?type ?name)))

(defn name-same-name [?name-a ?name-b]
  (logic/fresh [?string]
    (jdt/name-fully-qualified-name ?name-a ?string)
    (jdt/name-fully-qualified-name ?name-b ?string)))

(defn type-already-correctly-annotated [?type ?annotation]
  (logic/fresh [?type-argument ?type-name ?annotation-type-literal ?annotation-type-name]
    (jdt/ast :ParameterizedType ?type)
    (jdt/child :typeArguments ?type ?type-argument)
    (jdt/has :name ?type-argument ?type-name)
    (entity-annotation-type-literal ?annotation ?annotation-type-literal)
    (type-literal-name ?annotation-type-literal ?annotation-type-name)
    (name-same-name ?type-name ?annotation-type-name)))

(comment
(defn the-query [?field-declaration]
  (logic/fresh [?type-declaration ?field-type]
                       (annotation-named-entity ?annotation)
                       (annotation-field-declaration ?annotation ?field)
                       (damp.ekeko.jdt.basic/ast-encompassing-typedeclaration ?field ?type-declaration)
                       (is-type-declaration-astnode-subclass ?type-declaration)
                       (jdt/has :type ?field ?field-type)
                       (type-already-correctly-annotated ?field-type ?annotation))))
                   
