(ns 
  ^{:doc "Basic relations that are derived from the lower-level reification relations."
    :author "Coen De Roover"}
   damp.ekeko.jdt.basic
  (:refer-clojure :exclude [== type])
  (:use [clojure.core.logic])
  (:use [damp.ekeko.logic])
  (:use [damp.ekeko.jdt.reification])
  (:require [damp.ekeko.jdt [astnode :as astnode]])
  (:require [clojure set])
  (:import 
    [org.eclipse.jdt.core  ICompilationUnit IType  IJavaElement IMember]
    [org.eclipse.jdt.core.dom IBinding AST ASTParser TypeDeclaration Name FieldAccess SuperFieldAccess ClassInstanceCreation ConstructorInvocation SuperConstructorInvocation SuperMethodInvocation MethodInvocation QualifiedName  SimpleName Type CompilationUnit ASTNode ASTNode$NodeList ImportDeclaration Modifier IPackageBinding ITypeBinding IVariableBinding IMethodBinding IAnnotationBinding IMemberValuePairBinding]))
  
   
;; AST Nodes
;; ---------

;; General

(defn 
  ast-location 
  "Relation between ASTNode ?ast and Clojure vector ?locationVector 
  representing its location: [begin-character-index-in-file, 
                              end-chararacter-index-in-file, 
                              begin-line-number, 
                              end-line-number]
  "
  [?ast ?locationVector]
  (fresh [?root]
         (ast-root ?ast ?root)
         (equals ?locationVector
                 (let [astStart (.getStartPosition ^ASTNode ?ast)
                       astEnd (+ astStart (.getLength ^ASTNode ?ast))
                       astLineStart (.getLineNumber ^CompilationUnit ?root astStart)
                       astLineEnd (.getLineNumber ^CompilationUnit ?root astEnd)]
                   [astStart astEnd astLineStart astLineEnd]))))

(defn 
  ast-encompassing-typedeclaration
  "Relation between ASTNode ?ast and the TypeDeclaration ?t that encompasses it."
  ([?ast ?t] 
    (fresh [?keyw ?parent]
           (ast ?keyw ?ast)
           (ast-parent ?ast ?parent)
           (ast-encompassing-typedeclaration ?ast ?t ?parent)))           
  ([?ast ?t ?ancestor]
    (conde [(ast :TypeDeclaration ?ancestor) (== ?ancestor ?t)]
           [(fresh [?parent]
                   (ast-parent ?ancestor ?parent)
                   (ast-encompassing-typedeclaration ?ast ?t ?parent))])))

;TODO: implement a parent+ analogous to child+
(defn 
  ast-encompassing-method-non-failing 
  "Relation between ASTNode ?ast and either the MethodDeclaration ?m that encompasses it, 
   or nil if there is no encopassing MethodDeclaration.

   Operationally, performs a recursive ascend.
 
   See also: 
   Predicate ast-encompassing-method/2 which fails if there is no encompassing method."
  ([?ast ?m-or-nil] 
    (fresh [?keyw ?parent]
           (ast ?keyw ?ast)
           (ast-parent ?ast ?parent)
           (ast-encompassing-method-non-failing ?ast ?m-or-nil ?parent)))           
  ([?ast ?m ?ancestor]
    (conde [(ast :TypeDeclaration ?ancestor) (== nil ?m)]
           [(ast :MethodDeclaration ?ancestor) (== ?ancestor ?m)]
           [(fresh [?parent]
                   (ast-parent ?ancestor ?parent)
                   (ast-encompassing-method-non-failing ?ast ?m ?parent))])))

(defn 
  ast-encompassing-method
  "Relation between ASTNode ?ast and the MethodDeclaration ?m that encompasses it.

   Operationally, performs a recursive ascend.
 
   See also: 
   Predicate ast-encompassing-method-non-failing/2 unifies ?m with nil
   if there is no encompassing method."
  [?ast ?m]
  (all
    (!= nil ?m)
    (ast-encompassing-method-non-failing ?ast ?m)))


;; Modifier

(defn 
  modifier-static 
  "Reifies Modifier instances that correspond to the keyword static."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isStatic ^Modifier ?mod))))

(defn
  modifier-public
  "Reifies Modifier instances that correspond to the keyword public."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isPublic ^Modifier ?mod))))

(defn 
  modifier-protected
  "Reifies Modifier instances that correspond to the keyword protected."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isProtected ^Modifier ?mod))))

(defn
  modifier-private 
  "Reifies Modifier instances that correspond to the keyword private."  
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isPrivate ^Modifier ?mod))))

(defn
  modifier-abstract
  "Reifies Modifier instances that correspond to the keyword abstract."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isAbstract ^Modifier ?mod))))

(defn
  modifier-final
  "Reifies Modifier instances that correspond to the keyword final."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isFinal ^Modifier ?mod))))

(defn 
  modifier-native
  "Reifies Modifier instances that correspond to the keyword native."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isNative ^Modifier ?mod))))

(defn 
  modifier-synchronized 
  "Reifies Modifier instances that correspond to the keyword synchronized."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isSynchronized ^Modifier ?mod))))

(defn
  modifier-transient
  "Reifies Modifier instances that correspond to the keyword transient."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isTransient ^Modifier ?mod))))

(defn 
  modifier-volatile
  "Reifies Modifier instances that correspond to the keyword volatile."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isVolatile ^Modifier ?mod))))

(defn
  modifier-strictfp
  "Reifies Modifier instances that correspond to the keyword strictfp."
  [?mod]
  (all
    (ast :Modifier ?mod)
    (succeeds (.isStrictfp ^Modifier ?mod))))

(defn 
  ast-declaration-modifier
  "Reifies the relation between a declaration AST node ?ast 
   of kind ?key and any of its modifiers ?mod.

   See also: 
   Ternary predicate ast/3"
  [?key ?ast ?mod]
  (all
    (contains [:AnnotationTypeMemberDeclaration :FieldDeclaration :MethodDeclaration :SingleVariableDeclaration :TypeDeclaration :EnumDeclaration :EnumConstantDeclaration :AnnotationTypeDeclaration] ?key)
    (ast-declaration ?key ?ast)
    (child :modifiers ?ast ?mod)))
          


;; Relation between ASTNodes and IJavaElements from the Eclipse Java Model
;; -----------------------------------------------------------------------


;TODO: set *queried-project-models* to speed this up
(defn 
  ast-model
  [?keyword ?node ?model]
  ;THIS IS TOO SLOW, FOR NOW ASSUMING there is only 1 ekeko model, the SOOT-enabled one
  ;(fresh [?cu ?p]
  ;    (ast-root ?node ?cu)
  ;    (equals ?p (-> ?cu (.getJavaElement) (.getJavaProject)))
  ;    (equals ?model (.getJavaProjectModel (ekeko-model) ?p))))
  (ast ?keyword ?node))

(defn 
  ast-project
  "Relation between an ASTNode ?ast and the IJavaProject 
   in which it resides."
  [?ast ?project]
  (fresh [?root]
    (ast-root ?ast ?root)
    (equals ?project (.getJavaProject ^ICompilationUnit (.getJavaElement ^CompilationUnit ?root)))))

            
;; IBinding
;; --------
; these predicates are non-relational as it is difficult to quantify over all possible IBindings


(defn 
  is-binding-package?
  "Non-relational. Verifies that ?binding is an IPackageBinding."
  [?binding]
  (all 
    (succeeds (astnode/binding-package? ?binding))))

(defn 
  is-binding-type? 
   "Non-relational. Verifies that ?binding is a ITypeBinding."
  [?binding]
  (all 
    (succeeds (astnode/binding-type? ?binding))))

(defn 
  is-binding-variable?
  "Non-relational. Verifies that ?binding is an IVariableBinding."
  [?binding]
  (all 
    (succeeds (astnode/binding-variable? ?binding))))

(defn 
  is-binding-variable-field?
  "Non-relational. Verifies that ?binding is an IVariableBinding for a field."
  [?binding]
  (all 
    (is-binding-variable? ?binding)
    (succeeds (.isField ^IVariableBinding ?binding))))
  
(defn 
  is-binding-method? 
  "Non-relational. Verifies that ?binding is an IMethodBinding."
  [?binding]
  (all 
    (succeeds (astnode/binding-method? ?binding))))

(defn 
  is-binding-annotation?
  "Non-relational. Verifies that ?binding is an IAnnotationBinding."
  [?binding]
  (all 
    (succeeds (astnode/binding-annotation? ?binding))))

(defn
  is-binding-member-value-pair?
  "Non-relational. Verifies that ?binding is an IMemerValuePairBinding."
  [?binding]
  (all 
    (succeeds (astnode/binding-member-value-pair? ?binding))))

(defn 
  packagebinding-qualifiedname
  "Non-relational. Unifies ?name with the name of IPackageBinding ?bindingp."
  [?bindingp ?name] 
  (all 
    (equals ?name (.getName ^IPackageBinding ?bindingp))))

(defn
  typebinding-qualifiedname 
  "Non-relational. Unifies ?name with the fully qualified
   name of ITypeBinding ?binding."
  [?binding ?name]
  (all 
    (equals ?name (.getQualifiedName ^ITypeBinding ?binding))))

(defn
  typebinding-simplename 
  "Non-relational. Unifies ?name with the unqualified name 
   of ITypeBinding ?binding."
  [?binding ?name]
  (all 
    (equals ?name (.getName ^ITypeBinding ?binding))))

(defn 
  typebinding-erasurebinding
  "Non-relational. Unifies ?erasedtbinding with the erasure of
   ITypeBinding ?tbinding."
  [?tbinding ?erasedtbinding]
  (all 
    (equals ?erasedtbinding (.getErasure ^ITypeBinding ?tbinding))))

(defn 
  typebinding-extends-typebinding
  "Non-relational. Unifies ?supertbinding with the super type 
   of ITypeBinding ?tbinding."  
  [?tbinding ?supertbinding]
  (all
    (!= nil ?supertbinding) ;primitive types etc
    (equals ?supertbinding (.getSuperclass ^ITypeBinding ?tbinding))))

(defn 
  typebinding-implements-typebinding 
  "Non-relational. Unifies ?interfacetbinding with one of the 
   interface ITypeBindings implemented by ITypeBinding ?tbinding."  
  [?tbinding ?interfacetbinding]
  (fresh [?interfacetbindings]
         (equals ?interfacetbindings (.getInterfaces ^ITypeBinding ?tbinding))
         (contains ?interfacetbindings ?interfacetbinding)))

(defn 
  typebinding-packagebinding 
  "Non-relational. Unifies ?bindingp with the IPackageBinding 
   that declares ITypeBinding ?bindingt."  
  [?bindingt ?bindingp]
  (all 
    (!= nil ?bindingp)
    (equals ?bindingp (.getPackage ^ITypeBinding ?bindingt))))

(defn 
  typebinding-declaring-typebinding
  "Non-relational. Unifies ?declaringb with the ITypeBinding that 
   declares ITypeBinding ?tbinding.
   Nil for top-level types."  
  [?tbinding ?declaringb]
  (all 
    (equals ?declaringb (.getDeclaringClass ^ITypeBinding ?tbinding))))  
     
(defn 
  typebinding-declaring-methodbinding
   "Non-relational. Unifies ?declaringb with the IMethodBinding that
    declares ITypeBinding ?tbinding.
    Nil for top-level types."  
  [?tbinding ?declaringb]
  (all 
    (equals ?declaringb (.getDeclaringMethod ^ITypeBinding ?tbinding))))  

(defn 
  typebinding-javaelement
  "Non-relational. Unifies ?itype with the IType (a Java model element)
   corresponding to this ITypeBinding ?tbinding.
   Nil for top-level types."  
  [?tbinding ?itype]
   (all 
     (!= nil ?itype)
     (equals ?itype (.getJavaElement ^ITypeBinding ?tbinding))))

(defn 
  is-typebinding-from-source?
  "Non-relational. Verifies that ITypeBinding ?binding originates 
   from a source file."
  [?binding]
  (all 
    (equals true (.isFromSource ^ITypeBinding ?binding))))

(defn 
  is-typebinding-from-binary? 
  "Non-relational. Verifies that ITypeBinding ?binding originates 
   from a binary file."
  [?binding]
  (all 
    (equals false (.isFromSource ^ITypeBinding ?binding))))

(defn
  is-typebinding-primitive?
  "Non-relational. Verifies that ITypeBinding ?binding represents 
   a primitive type."
  [?binding]
  (all
    (equals true (.isPrimitive ^ITypeBinding ?binding))))

(defn 
  is-typebinding-reference? 
  "Non-relational. Verifies that ITypeBinding ?binding represents
   a reference type."
  [?binding]
  (all
    (equals false (.isPrimitive ^ITypeBinding ?binding))))

(defn 
  methodbinding-declaring-typebinding
  "Non-relational. Unifies ?tbinding with the ITypeBinding that
   declares IMethodBinding ?mbinding."
  [?mbinding ?tbinding]
  (all
    (!= ?tbinding nil)
    (equals ?tbinding (.getDeclaringClass ^IMethodBinding ?mbinding))))

(defn 
  methodbinding-packagebinding
  "Non-relational. Unifies ?pbinding with the IPackageBinding in
   which IMethodBinding ?mbinding is declared."
  [?mbinding ?pbinding]
  (fresh [?tbinding]
         (methodbinding-declaring-typebinding ?mbinding ?tbinding)
         (typebinding-packagebinding ?tbinding ?pbinding)))

(defn 
  is-methodbinding-from-source?
  "Non-relational. Verifies whether IMethodBinding ?binding 
   originates from a source file."
  [?binding]
  (fresh [?typebinding]
         (methodbinding-declaring-typebinding ?binding ?typebinding)
         (is-typebinding-from-source? ?typebinding)))
                       
(defn 
  is-methodbinding-from-binary?
  "Non-relational. Verifies whether IMethodBinding ?binding
   originates from a binary file."
  [?binding]
  (fresh [?typebinding]
         (methodbinding-declaring-typebinding ?binding ?typebinding)
         (is-typebinding-from-binary? ?typebinding)))

(defn 
  methodbinding-signature
  "Non-relational. Unifies ?sig with the signature string
   of IMethodBinding ?binding."
  [?binding ?sig]
  (all
    (equals ?sig (damp.ekeko.JavaProjectModel/sootSignatureForMethodBinding ?binding))))

(defn 
  field-variablebinding-declaring-typebinding 
  "Non-relational. Unifies ?tbinding with the ITypeBinding that 
   declares field IVariableBinding ?fvbinding."
  [?fvbinding ?tbinding]
   (all
     (!= nil ?tbinding) 
     (equals ?tbinding (.getDeclaringClass ^IVariableBinding ?fvbinding))))

(defn 
  field-variablebinding-signature 
  "Non-relational. Unifies ?sig with the signature string of
   field IVariableBinding ?fvbinding."
  [?fvbinding ?sig]
   (all
      (equals ?sig (damp.ekeko.JavaProjectModel/sootSignatureForFieldVariableBinding ?fvbinding))))


(defn 
  import-declaration-imports-binding
  "Relation between an import declaration ASTNode ?import and 
   the IBinding ?binding it resolves to."
  [?import ?binding]
  (all
    (ast :ImportDeclaration ?import)
    (equals ?binding (.resolveBinding ^ImportDeclaration ?import))))

(defn
  import-declaration-imports-package
  "Relation between an import declaration ASTNode ?import 
   and the IPackageBinding it imports directly (for package 
   imports) or indirectly (for type imports and static 
   imports of a field or a method)."
  [?import ?packagebinding]
  (fresh [?binding]
    (import-declaration-imports-binding ?import ?binding)
    (conda [(is-binding-package? ?binding) 
            (== ?binding ?packagebinding)]
           [(is-binding-type? ?binding)
            (equals ?packagebinding (.getPackage ^ITypeBinding ?binding))] 
           ;non-dynamic, static import of field and method
           [(is-binding-method? ?binding)
            (equals ?packagebinding (.getPackage ^ITypeBinding (.getDeclaringClass ^IMethodBinding ?binding)))]
           [(is-binding-variable? ?binding)
            (equals ?packagebinding (.getPackage ^ITypeBinding (.getDeclaringClass ^IVariableBinding ?binding)))])))



;; Java Model Elements
;; -------------------

(defn
  type-representing-annotation
  "Relation of IType instances ?t that represent an annotation type."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isAnnotation ^IType ?t))))

(defn
  type-representing-anonymous
  "Relation of IType instances ?t that represent an anonymous type."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isAnonymous ^IType ?t))))

(defn
  type-representing-class
  "Relation of IType instances ?t that represent a class."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isClass ^IType ?t))))

(defn
  type-representing-enum
  "Relation of IType instances ?t that represent an enumeration class."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isEnum ^IType ?t))))

(defn
  type-representing-interface
  "Relation of IType instances ?t that represent an interface."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isInterface ^IType ?t))))

(defn
  type-representing-local
  "Relation of IType instances ?t that represent a local type."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isLocal ^IType ?t))))

(defn
  type-representing-member
  "Relation of IType instances ?t that represent a member type."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isMember ^IType ?t))))

(defn
  type-representing-resolved
  "Relation of IType instances ?t that represent a resolved type."
  [?t]
  (all 
    (type ?t)
    (succeeds (.isResolved ^IType ?t))))


