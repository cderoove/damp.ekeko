(ns 
  ^{:doc "Relations between ASTNode and IBinding instances.
          If possible, use namespaces damp.ekeko.jdt.ast, damp.ekeko.jdt.structure and damp.ekeko.jdt.aststructure instead."
    :author "Coen De Roover"}
damp.ekeko.jdt.astbindings
  (:refer-clojure :exclude [== type])
  (:require [clojure.core [logic :as l]])
  (:require 
    [damp.ekeko [logic :as el] [ekekomodel :as ekekomodel]])
  (:require
    [damp.ekeko.jdt [ast :as ast] [bindings :as bindings] [javaprojectmodel :as javaprojectmodel]])
  (:import 
    [org.eclipse.jdt.core.dom Name Annotation SuperConstructorInvocation ConstructorInvocation ClassInstanceCreation SuperMethodInvocation MethodInvocation Expression ImportDeclaration Type TypeDeclaration QualifiedName SimpleName SuperFieldAccess FieldAccess IBinding IPackageBinding ITypeBinding IVariableBinding IMethodBinding IAnnotationBinding IMemberValuePairBinding]))


;; Relation between ASTNode and IBinding
;; ------------------------------------

(defn
  ast|annotation-binding|annotation
  "Relation between an Annotation AST node and the IAnnotationBinding it resolves to."
  [?annotation ?binding]
  (l/all
    (ast/ast :Annotation ?annotation)
    (l/!= nil ?binding)
    (el/equals ?binding (.resolveAnnotationBinding ^Annotation ?annotation))))

(defn
  ast|expression-binding|type
  "Relation between an Expression instance ?ast,
   the keyword ?key representing its kind,
   and the ITypeBinding ?binding for its type."
  [?key ?ast ?binding]
  (l/all
    (ast/ast :Expression ?ast)
    (l/!= nil ?binding)
    (el/equals ?binding (.resolveTypeBinding ^Expression ?ast))
    (ast/ast ?key ?ast)))


(defn 
  ast|type-binding|type
   "Relation between a type ASTNode ?type, the keyword ?key 
    corresponding to its kind, and the ITypeBinding it resolves to.
   
   See also: 
   binary predicate ast|type/2"
  [?key ?type ?binding]
  (l/all 
    (ast/ast|type ?key ?type)
    (l/!= nil ?binding)
    (el/equals ?binding (.resolveBinding ^Type ?type))))


(defn
  ast|name-binding
  "Relation between a Name ?name, the keyword ?key 
  corresponding to its kind (SimpleName or QualifiedName),
  and the IBinding it resolves to."
  [?key ?name ?binding]
  (l/all 
    (ast/ast :Name ?name)
    (ast/ast ?key ?name) 
    (l/!= nil ?binding)
    (el/equals ?binding (.resolveBinding ^Name ?name))))

(defn
  ast|localvariable-binding|variable
  "Relation between a local variable ?ast (either a SimpleName or QualifiedName), 
   the keyword ?key corresponding to its kind, and the IVariableBinding it resolves to."
  [?key ?var ?binding]
  (l/all
    (ast/ast|localvariable ?key ?var)
    (ast|name-binding ?key ?var ?binding)))


(defn
  ast|typedeclaration-binding|type
  "Relation between a TypeDeclaration instance ?typeDeclaration
   and the ITypeBinding instance ?binding it resolves to.
   Note that ?binding is required to differ from ?nil. 
   
   See also:
   API documentation of org.eclipse.jdt.core.dom.TypeDeclaration
   and org.eclipse.jdt.core.dom.ITypeBinding"
  [?typeDeclaration ?binding]
  (l/all 
    (ast/ast :TypeDeclaration ?typeDeclaration)
    (l/!= nil ?binding)
    (el/equals ?binding (.resolveBinding ^TypeDeclaration ?typeDeclaration))))

(defn 
  ast|importdeclaration-binding
  "Relation between an import declaration ASTNode ?import and 
   the IBinding ?binding it resolves to."
  [?import ?binding]
  (l/all
    (ast/ast :ImportDeclaration ?import)
    (el/equals ?binding (.resolveBinding ^ImportDeclaration ?import))))

(defn
   ast|importdeclaration-binding|package
   "Relation between an import declaration ASTNode ?import 
   and the IPackageBinding it imports directly (for package 
   imports) or indirectly (for type imports and static 
   imports of a field or a method)."
  [?import ?packagebinding]
  (l/fresh [?binding]
         (ast|importdeclaration-binding ?import ?binding)
         (l/conda [(bindings/is-binding-package? ?binding) 
                 (l/== ?binding ?packagebinding)]
                [(bindings/is-binding-type? ?binding)
                 (el/equals ?packagebinding (.getPackage ^ITypeBinding ?binding))] 
                ;non-dynamic, static import of field and method
                [(bindings/is-binding-method? ?binding)
                 (el/equals ?packagebinding (.getPackage ^ITypeBinding (.getDeclaringClass ^IMethodBinding ?binding)))]
                [(bindings/is-binding-variable? ?binding)
                 (el/equals ?packagebinding (.getPackage ^ITypeBinding (.getDeclaringClass ^IVariableBinding ?binding)))])))

(defprotocol 
  IResolveToFieldBinding
  (binding-for-fieldaccesss-like-node [n]))

(extend-protocol 
  IResolveToFieldBinding
  FieldAccess
  (binding-for-fieldaccesss-like-node [n] (.resolveFieldBinding n)) 
  SuperFieldAccess
  (binding-for-fieldaccesss-like-node [n] (.resolveFieldBinding n))
  SimpleName
  (binding-for-fieldaccesss-like-node [n] (.resolveBinding n))
  QualifiedName
  (binding-for-fieldaccesss-like-node [n] (.resolveBinding n)))

(defn
  ast|fieldaccess-binding|variable
  "Relation between a field accessing ASTNode ?node, the keyword 
   ?key corresponding to its kind, and the IVariableBinding it resolves to.
   
   See also: 
   binary predicate ast|fieldaccess/2"
  [?key ?node ?binding]
  (l/all 
    (ast/ast|fieldaccess ?key ?node)
    (l/!= nil ?binding)
    (el/equals ?binding (binding-for-fieldaccesss-like-node ?node))))

(defn 
  ast|declaration-binding
  "Relation between a declaration ASTNode ?n,
   the keyword ?key corresponding to its kind, and the IBinding it resolves to.
   
   Note that this relation is quite slow to compute.

   See also:
   ternary predicate typedeclaration-binding|type/3 
   which restricts ?n to TypeDeclaration instances"
  [?key ?n ?binding]
  (l/all 
    (l/!= ?binding nil)
    (l/!= ?n nil)
    (l/conda
      [(el/v+ ?binding)
       (l/all
         (el/equals ?n (javaprojectmodel/binding-to-declaration ?binding))
         (ast/ast ?key ?n))]
      [(el/v- ?binding) 
       (l/all
         (ast/ast|declaration|resolveable ?key ?n)
         (el/equals ?binding (.resolveBinding ?n)))]))) ;no type hint possible, no common super class for nodes with such a method
   
(defprotocol 
  IResolveToMethodBinding
  (binding-for-invocation-like-node [n]))

(extend-protocol 
  IResolveToMethodBinding
  MethodInvocation
  (binding-for-invocation-like-node [n] (.resolveMethodBinding n)) 
  SuperMethodInvocation
  (binding-for-invocation-like-node [n] (.resolveMethodBinding n))
  ClassInstanceCreation
  (binding-for-invocation-like-node [n] (.resolveConstructorBinding n))
  ConstructorInvocation
  (binding-for-invocation-like-node [n] (.resolveConstructorBinding n))
  SuperConstructorInvocation
  (binding-for-invocation-like-node [n] (.resolveConstructorBinding n)))
    
(defn 
  ast|invocation-binding|method
  "Relation between an ASTNode ?node that invokes a method or constructor, 
   the keyword ?key corresponding to its kind, 
   and the IMethodBinding it resolves to.
   
   See also: 
   binary predicate ast-invocation/2"
  [?key ?node ?binding]
  (l/all 
    (ast/ast|invocation ?key ?node)
    (l/!= nil ?binding)
    (el/equals ?binding (binding-for-invocation-like-node ?node))))
