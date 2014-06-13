(ns 
  ^{:doc "Collection of occasionally convenient, but non-essential predicates. "
    :author "Coen De Roover"}
  damp.ekeko.jdt.convenience
  (:refer-clojure :exclude [== type])
  (:require [clojure.core [logic :as l]])
  (:require [damp.ekeko [logic :as el]])
  (:require [damp.ekeko.jdt [ast :as ast] [astbindings :as astbindings] [aststructure :as aststructure]])
  (:import [org.eclipse.jdt.core.dom Modifier IAnnotationBinding ITypeBinding]))

;; Modifiers
;; ---------

(defn 
  modifier|static 
  "Reifies Modifier instances that correspond to the keyword static."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isStatic ^Modifier ?mod))))

(defn
  modifier|public
  "Reifies Modifier instances that correspond to the keyword public."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isPublic ^Modifier ?mod))))

(defn 
  modifier|protected
  "Reifies Modifier instances that correspond to the keyword protected."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isProtected ^Modifier ?mod))))

(defn
  modifier|private 
  "Reifies Modifier instances that correspond to the keyword private."  
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isPrivate ^Modifier ?mod))))

(defn
  modifier|abstract
  "Reifies Modifier instances that correspond to the keyword abstract."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isAbstract ^Modifier ?mod))))

(defn
  modifier|final
  "Reifies Modifier instances that correspond to the keyword final."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isFinal ^Modifier ?mod))))

(defn 
  modifier|native
  "Reifies Modifier instances that correspond to the keyword native."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isNative ^Modifier ?mod))))

(defn 
  modifier|synchronized 
  "Reifies Modifier instances that correspond to the keyword synchronized."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isSynchronized ^Modifier ?mod))))

(defn
  modifier|transient
  "Reifies Modifier instances that correspond to the keyword transient."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isTransient ^Modifier ?mod))))

(defn 
  modifier|volatile
  "Reifies Modifier instances that correspond to the keyword volatile."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isVolatile ^Modifier ?mod))))

(defn
  modifier|strictfp
  "Reifies Modifier instances that correspond to the keyword strictfp."
  [?mod]
  (l/all
    (ast/ast :Modifier ?mod)
    (el/succeeds (.isStrictfp ^Modifier ?mod))))



;; Type declaration members and identifiers
;; ----------------------------------------

(defn
  typedeclaration-identifier
  "Relation between a type declaration and the identifier 
	  string of its simple name."
  [?typedec ?typeid]
  (l/fresh [?tname]
           (ast/ast :TypeDeclaration ?typedec)
           (ast/has :name ?typedec ?tname)
           (ast/name|simple-string ?tname ?typeid)))

(defn
  typedeclaration-identifier-bodydeclaration-identifier
  "Relation between a type declaration and one of its member declarations,
	   and the identifier strings of their respective simple names. Note that 
	   a single field declaration can introduce multiple identifiers." 
  [?typedec ?typeid ?memberdec ?memberid]
  (l/all
    (typedeclaration-identifier ?typedec ?typeid)
    (ast/child :bodyDeclarations ?typedec ?memberdec)
    (l/fresh [?named]
             (l/conda [(ast/ast :FieldDeclaration ?memberdec)
                       (ast/child :fragments ?memberdec ?named)]
                      [l/s# 
                       (l/== ?named ?memberdec)])
             (l/fresh [?mname]
                      (ast/has :name ?named ?mname)
                      (ast/name|simple-string ?mname ?memberid)))))


(defn
  typedeclaration-identifier-typedeclaration-identifier
  "Relation between a type declaration and one of its member 
	   type declarations, and the identifier strings 
	   of their respective simple names." 
  [?typedec ?typeid ?memberdec ?memberid]
  (l/all
    (typedeclaration-identifier-bodydeclaration-identifier ?typedec ?typeid ?memberdec ?memberid)
    (ast/ast :TypeDeclaration ?memberdec)))

(defn
  typedeclaration-identifier-methoddeclaration-identifier
  "Relation between a type declaration and one of its member 
	   method declarations, and the identifier strings 
	   of their respective simple names." 
  [?typedec ?typeid ?memberdec ?memberid]
  (l/all
    (typedeclaration-identifier-bodydeclaration-identifier ?typedec ?typeid ?memberdec ?memberid)
    (ast/ast :MethodDeclaration ?memberdec)))

(defn
  typedeclaration-identifier-fielddeclaration-identifier
  "Relation between a type declaration and one of its member 
	   field declarations, and the identifier strings 
	   of the type and one of the variable declaration fragments of the field."
  [?typedec ?typeid ?memberdec ?memberid]
  (l/all
    (typedeclaration-identifier-bodydeclaration-identifier ?typedec ?typeid ?memberdec ?memberid)
    (ast/ast :FieldDeclaration ?memberdec)))


;; MethodDeclaration
;; -----------------

(defn methodinvocation|name 
  "Relation between a MethodInvocation and its name."
  [?methodinvoc ?name]
  (l/all
    (ast/ast :MethodInvocation ?methodinvoc)
    (ast/has :name ?methodinvoc ?name)))


(defn methodinvocation|named
  "Relation between a MethodInvocation and the String representation of its name"
  [?methodinvoc ?string]
  (l/fresh [?name]
    (methodinvocation|name ?methodinvoc ?name)
    (ast/name|simple-string ?name ?string)))


;; Misc
;; ----

(defn 
  annotation-name|qualified|string 
  "Relation between an annotation and its qualified name as a string."
  [?annotation ?name]
  (l/fresh [?annotation-binding ?type-binding]
           (ast/ast :Annotation ?annotation)
           (astbindings/ast|annotation-binding|annotation ?annotation ?annotation-binding)
           (el/equals ?type-binding (.getAnnotationType ^IAnnotationBinding ?annotation-binding))
           (el/equals ?name (.getQualifiedName ^ITypeBinding ?type-binding))))

(defn
  fielddeclaration-annotation 
  "Relation between a FieldDeclaration and one of its Annotation."
  [?fielddecl ?annotation]
  (l/all
    (ast/ast :Annotation ?annotation)
    (ast/ast-parent ?annotation ?fielddecl)
    (ast/ast :FieldDeclaration ?fielddecl)))

(defn 
  typeliteral-ast|type
  "Relation between a TypeLiteral (e.g., Integer.class) and 
	   its Type AST node (not a resolved type!).
	
	   See typeliteral-type/2 for resolved types. "
  [?literal ?type]
  (l/all
    (ast/ast :TypeLiteral ?literal)
    (ast/has :type ?literal ?type)))

(defn
  typeliteral-type
  "Relation between a TypeLiteral (e.g., Integer.class) and its Type (e.g., java.lang.Integer)."
  [?literal ?type]
  (l/fresh [?ast ?key]
           (typeliteral-ast|type ?literal ?ast)
           (aststructure/ast|type-type ?key ?ast ?type)))




