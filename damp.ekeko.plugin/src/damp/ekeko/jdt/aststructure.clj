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
  ([?key ?ast ?type]
    (l/fresh [?binding]
             (astbindings/ast|type-binding|type ?key ?ast ?binding)
             (bindings/binding-element ?binding ?type)))
  ([?ast ?type]
    (l/fresh [?key]
             (ast|type-type ?key ?ast ?type))))
             
(defn
  ast|annotation-type 
  "Relation between an Annotation ?ast and the IType ?type it refers to."
  ([?ast ?type]
    (l/fresh [?abinding ?tbinding]
             (astbindings/ast|annotation-binding|annotation ?ast ?abinding)
             (el/equals ?tbinding (.getAnnotationType ?abinding))
             (bindings/binding-element ?tbinding ?type)))
  ([?key ?ast ?type]
    (l/all 
      (ast|annotation-type ?ast ?type)
      (ast/ast ?key ?ast))))

(defn
  ast|expression-type 
  "Relation between an Expression ?ast and its declared IType ?type.
   Fails for primitive-valued expressions."
  ([?key ?ast ?type]
    (l/fresh [?tbinding]
      (astbindings/ast|expression-binding|type ?key ?ast ?tbinding)
      (bindings/binding-element ?tbinding ?type)))
  ([?ast ?type]
    (l/fresh [?key]
             (ast|expression-type ?key ?ast ?type))))


(defn
  ast|expression-type|primitive
  "Relation or primitive-valued Expression ?ast and a string ?typestring
   corresponding to the keyword for their primitive type (e.g., int)."
  ([?ast ?typestring]
    (l/fresh [?key]
             (ast|expression-type|primitive ?key ?ast ?typestring)))
  ([?key ?ast ?typestring]
     (l/fresh [?tbinding]
              (astbindings/ast|expression-binding|type ?key ?ast ?tbinding)
              (bindings/is-typebinding-primitive? ?tbinding)
              (bindings/typebinding-simplename ?tbinding ?typestring))))


            
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
  ast|fieldaccess-ast|variabledeclaration
  "Relation between fieldaccess-like ASTNode (i.e., SimpleName, FieldAccess, QualifiedName)
   and the VariableDeclaration(Fragment) it refers to lexically."
  [?var ?dec]
  (l/fresh [?k-var ?k-dec ?b] 
           (astbindings/ast|fieldaccess-binding|variable ?k-var ?var ?b)
           (astbindings/ast|declaration-binding ?k-dec ?dec ?b)))


(defn
  ast|fieldaccess-ast|referred
  "Relation between a fieldaccess-like ASTNode (i.e., SimpleName, FieldAccess, QualifiedName)
   and one of the ASTNodes it refers to lexically (i.e., SimpleName, VariableDeclarationFragment, FieldDeclaration)."
  [?var ?dec]
  (l/fresh [?vardecfragment]
         (ast|fieldaccess-ast|variabledeclaration ?var ?vardecfragment)
         (l/conde
           [(l/== ?vardecfragment ?dec)]
           [(ast/ast-parent ?vardecfragment ?dec)] ;fielddeclaration
           [(ast/has :name ?vardecfragment ?dec)]))) ;name

(defn
  ast|localvariable-ast|variabledeclaration 
  "Relation between a local variable ASTNode (i.e., SimpleName or QualifiedName used as an expression) 
   and the (Single)VariableDeclaration it refers to lexically."
  [?var ?dec]
  (l/fresh [?k-var ?k-dec ?b] 
           (astbindings/ast|localvariable-binding|variable ?k-var ?var ?b) 
           (astbindings/ast|declaration-binding ?k-dec ?dec ?b)))
           

(defn
  ast|localvariable-ast|referred
  "Relation between a local variable ASTNode (i.e., SimpleName or QualifiedName used as an expression) 
   and one of the ASTNodes it refers to lexically (i.e., SimpleName or (Single)VariableDeclaration(Fragment), VariableDeclarationExpression, VariableDeclarationStatement)."
  [?var ?dec]
  (l/fresh [?vardec]
           (ast|localvariable-ast|variabledeclaration ?var ?vardec)
           (l/conde
             [(l/== ?vardec ?dec)]
             [(ast/has :name ?vardec ?dec)]
             [(ast/ast :VariableDeclarationFragment ?vardec) ;excludes SingleVariableDeclaration, only ok for VariableDeclaraitonFragment
              (ast/ast-parent ?vardec ?dec)])))
  

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
            (l/!= nil ?type)
            (el/equals ?ast (javaprojectmodel/itype-to-declaration ?type))
            (l/!= nil ?ast)]))

(defn 
  typedeclaration-typedeclaration|super 
  "Relation between a TypeDeclaration AST and a TypeDeclaration that declares 
   one of its super types (if available from source)."
  [?typedeclaration ?supertypedeclaration]
  (l/fresh [?type ?type|super]
    (typedeclaration-type ?typedeclaration ?type)
    (structure/type-type|super+ ?type ?type|super)
    (typedeclaration-type ?supertypedeclaration ?type|super)))

(defn
  methoddeclaration-methoddeclaration|overrides
  "Relation between a MethodDeclaration ?m and one of its overriding MethodDeclatations ?overrider."
  [?m ?overrider]
  (l/fresh [?overriders]
    (ast/ast :MethodDeclaration ?m)
    (el/equals ?overriders (damp.ekeko.jdt.javaprojectmodel/method-overriders ?m)) 
    (el/contains ?overriders ?overrider)
    (ast/ast :MethodDeclaration ?overrider)))

(defn
  methodinvocation-methoddeclaration
  "Relation between a MethodInvocation ?i and one of the possible MethodDeclarations ?m it may call."
  [?i ?m]
  (l/fresh [?targets]
    (ast/ast :MethodInvocation ?i)
    (el/equals ?targets (javaprojectmodel/invocation-targets ?i))
    (el/contains ?targets ?m)
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


  


