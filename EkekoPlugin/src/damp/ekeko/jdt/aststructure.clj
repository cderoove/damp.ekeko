(ns 
  damp.ekeko.jdt.aststructure
  (:refer-clojure :exclude [== type])
  (:require [clojure.core [logic :as l]])
  (:require 
    [damp.ekeko [logic :as el] [ekekomodel :as ekekomodel]]
    [damp.ekeko.jdt [ast :as ast] [structure :as structure] [bindings :as bindings]  [astbindings :as astbindings] [javaprojectmodel :as javaprojectmodel]])
  (:import 
    [org.eclipse.jdt.core IJavaElement ICompilationUnit]
    [org.eclipse.jdt.core.dom ASTParser AST IBinding CompilationUnit]
    ))

;; Basic Structural Relations
;; --------------------------

(defn 
  ast|type-type
  "Relation between a type ASTNode ?ast, its keyword kind ?key, and the IType ?type it refers to.

  See also:
  ast|type-binding|type/3 which resolves a type ASTNode to an ITypeBinding."
  [?key ?ast ?type]
  (l/fresh [?binding]
         (astbindings/ast|type-binding|type ?key ?ast ?binding)
         (bindings/binding-element ?binding ?type)))

(defn
   ast|importdeclaration-package
   "Relation between an import declaration ASTNode ?import 
   and the IPAckage it imports directly (for package 
   imports) or indirectly (for type imports and static 
   imports of a field or a method)."
   [?import ?package]
   (l/fresh [?binding]
            (astbindings/ast|importdeclaration-binding|package ?import ?binding)
            (bindings/binding-element ?binding ?package)))
   

(defn 
  typedeclaration-type
  "Relation between a TypeDeclaration ?ast and the IType ?type it refers to.

  See also:
  typedeclaration-binding|type/3 which resolves a type ASTNode to an ITypeBinding."
  [?ast ?type]
  (l/conda [(el/v- ?type)
          (l/fresh [?binding]
                 (astbindings/ast|typedeclaration-binding|type ?ast ?binding)
                 (bindings/binding-element ?binding ?type))]
         [(el/v+ ?type)
          (l/!= nil ?ast)
          (el/equals ?ast (javaprojectmodel/ielement-to-declaration ?type))]))

(defn 
  typedeclaration-typedeclaration|super 
  "Relation between a TypeDeclaration AST and a TypeDeclaration that declares 
   one of its super types (if available from source)."
  [?typedeclaration ?supertypedeclaration]
  (l/fresh [?type ?type|super]
    (typedeclaration-type ?typedeclaration ?type)
    (structure/type-type|super ?type ?type|super)
    (typedeclaration-type ?supertypedeclaration ?type|super)))

(defn
  methoddeclaration-methoddeclaration|overrides
  "Relation between a MethodDeclaration ?m and one of its overriding MethodDeclatations ?overrider."
  [?m ?overrider]
  (l/all
    (ast/ast :MethodDeclaration ?m)
    (el/contains (damp.ekeko.jdt.javaprojectmodel/method-overriders ?m) ?overrider)
    (ast/ast :MethodDeclaration ?overrider)))

(defn
  methodinvocation-methoddeclaration
  "Relation between a MethodInvocation ?i and one of the possible MethodDeclarations ?m it may call."
  [?i ?m]
  (l/all
    (ast/ast :MethodInvocation ?i)
    (el/contains (javaprojectmodel/invocation-targets ?i) ?m)
    (ast/ast :MethodDeclaration ?m)))

(defn 
  typedeclaration-name|qualified|string
  "Relation between a TypeDeclaration AST  and its qualified name as a string."
  [?type-decl ?name]
  (l/fresh [?itype]
         (typedeclaration-type ?type-decl ?itype)
         (structure/type-name|qualified|string ?itype ?name)))


(defn 
  ast-project
  "Relation between an ASTNode ?ast and the IJavaProject 
   in which it resides."
  [?ast ?project]
  (l/fresh [?root]
    (ast/ast-root ?ast ?root)
    (el/equals ?project (.getJavaProject ^ICompilationUnit (.getJavaElement ^CompilationUnit ?root)))))




