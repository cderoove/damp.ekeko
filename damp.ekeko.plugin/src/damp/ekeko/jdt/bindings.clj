(ns 
  ^{:doc "Mostly non-relational predicates for reasoning about Eclipse JDT IBinding instances.
          Not meant to be used by end-users."
    :author "Coen De Roover"}
  damp.ekeko.jdt.bindings
  (:refer-clojure :exclude [== type])
  (:require [clojure.core [logic :as l]])
  (:require [damp.ekeko [logic :as el]])
  (:require [damp.ekeko.jdt [astnode :as astnode]])
  (:import 
    [damp.ekeko JavaProjectModel]
    [org.eclipse.jdt.core IJavaElement]
    [org.eclipse.jdt.core.dom IBinding AST ASTParser SuperConstructorInvocation ConstructorInvocation ClassInstanceCreation SuperMethodInvocation MethodInvocation Expression ImportDeclaration Type TypeDeclaration QualifiedName SimpleName SuperFieldAccess FieldAccess IBinding IPackageBinding ITypeBinding IVariableBinding IMethodBinding IAnnotationBinding IMemberValuePairBinding]))


;; Link between IBinding and Element (not to be used by end-users)
;; ---------------------------------------------------------------

(defn- ibinding-for-ijavaelement [^IJavaElement ijavaelement]
  (let [^ASTParser parser (ASTParser/newParser JavaProjectModel/JLS)]                
    (.setProject  parser (.getJavaProject ijavaelement))
    (if-let [bindings (.createBindings parser (into-array IJavaElement [ijavaelement]) nil)]
      (first bindings))))
  
  
(defn 
  element-binding
  "Non-relational. Unifies ?ibinding with the IBinding 
   corresponding to the IJavaElement ?ijavaelement, if there is one.
   Not intended for use by end-users.

  See also:
  Binary predicate binding-element/2" 
  [?ijavaelement ?ibinding]
  (l/all 
    (l/!= nil ?ibinding)
    (el/equals ?ibinding (ibinding-for-ijavaelement ?ijavaelement))))        


(defn 
  binding-element
  "Non-relational. Unifies ?ijavaelement with the IJavaElement
   corresponding to the IBinding ?ibinding, if there is one.
   Not intended for use by end-users.

  See also:
  Binary predicate element-binding/2" 
  [?ibinding ?ijavaelement]
  (l/all
    (l/!= nil ?ijavaelement)
    (el/equals ?ijavaelement (.getJavaElement ^IBinding ?ibinding))))



;; IBinding Auxiliaries
;; --------
; these predicates are non-relational as it is difficult to quantify over all possible IBindings

(defn 
  is-binding-package?
  "Non-relational. Verifies that ?binding is an IPackageBinding."
  [?binding]
  (l/all 
    (el/succeeds (astnode/binding-package? ?binding))))

(defn 
  is-binding-type? 
   "Non-relational. Verifies that ?binding is a ITypeBinding."
  [?binding]
  (l/all 
    (el/succeeds (astnode/binding-type? ?binding))))

(defn 
  is-binding-variable?
  "Non-relational. Verifies that ?binding is an IVariableBinding."
  [?binding]
  (l/all 
    (el/succeeds (astnode/binding-variable? ?binding))))

(defn 
  is-binding-variable-field?
  "Non-relational. Verifies that ?binding is an IVariableBinding for a field."
  [?binding]
  (l/all 
    (is-binding-variable? ?binding)
    (el/succeeds (.isField ^IVariableBinding ?binding))))
  
(defn 
  is-binding-method? 
  "Non-relational. Verifies that ?binding is an IMethodBinding."
  [?binding]
  (l/all 
    (el/succeeds (astnode/binding-method? ?binding))))

(defn 
  is-binding-annotation?
  "Non-relational. Verifies that ?binding is an IAnnotationBinding."
  [?binding]
  (l/all 
    (el/succeeds (astnode/binding-annotation? ?binding))))

(defn
  is-binding-member-value-pair?
  "Non-relational. Verifies that ?binding is an IMemerValuePairBinding."
  [?binding]
  (l/all 
    (el/succeeds (astnode/binding-member-value-pair? ?binding))))

(defn 
  packagebinding-qualifiedname
  "Non-relational. Unifies ?name with the name of IPackageBinding ?bindingp."
  [?bindingp ?name] 
  (l/all 
    (el/equals ?name (.getName ^IPackageBinding ?bindingp))))

(defn
  typebinding-qualifiedname 
  "Non-relational. Unifies ?name with the fully qualified
   name of ITypeBinding ?binding."
  [?binding ?name]
  (l/all 
    (el/equals ?name (.getQualifiedName ^ITypeBinding ?binding))))

(defn
  typebinding-simplename 
  "Non-relational. Unifies ?name with the unqualified name 
   of ITypeBinding ?binding."
  [?binding ?name]
  (l/all 
    (el/equals ?name (.getName ^ITypeBinding ?binding))))

(defn 
  typebinding-erasurebinding
  "Non-relational. Unifies ?erasedtbinding with the erasure of
   ITypeBinding ?tbinding."
  [?tbinding ?erasedtbinding]
  (l/all 
    (el/equals ?erasedtbinding (.getErasure ^ITypeBinding ?tbinding))))

(defn 
  typebinding-typebinding|super
  "Non-relational. Unifies ?supertbinding with the super type 
   of ITypeBinding ?tbinding."  
  [?tbinding ?supertbinding]
  (l/all
    (l/!= nil ?supertbinding) ;primitive types etc
    (el/equals ?supertbinding (.getSuperclass ^ITypeBinding ?tbinding))))

(defn 
  typebinding-typebinding|interface 
  "Non-relational. Unifies ?interfacetbinding with one of the 
   interface ITypeBindings implemented by ITypeBinding ?tbinding."  
  [?tbinding ?interfacetbinding]
  (l/fresh [?interfacetbindings]
         (el/equals ?interfacetbindings (.getInterfaces ^ITypeBinding ?tbinding))
         (el/contains ?interfacetbindings ?interfacetbinding)))

(defn 
  typebinding-packagebinding 
  "Non-relational. Unifies ?bindingp with the IPackageBinding 
   that declares ITypeBinding ?bindingt."  
  [?bindingt ?bindingp]
  (l/all 
    (l/!= nil ?bindingp)
    (el/equals ?bindingp (.getPackage ^ITypeBinding ?bindingt))))

(defn 
  typebinding-typebinding|declaring
  "Non-relational. Unifies ?declaringb with the ITypeBinding that 
   declares ITypeBinding ?tbinding.
   Nil for top-level types."  
  [?tbinding ?declaringb]
  (l/all 
    (el/equals ?declaringb (.getDeclaringClass ^ITypeBinding ?tbinding))))  
     
(defn 
  typebinding-methodbinding|declaring
   "Non-relational. Unifies ?declaringb with the IMethodBinding that
    declares ITypeBinding ?tbinding.
    Nil for top-level types."  
  [?tbinding ?declaringb]
  (l/all 
    (el/equals ?declaringb (.getDeclaringMethod ^ITypeBinding ?tbinding))))  

(defn 
  typebinding-javaelement
  "Non-relational. Unifies ?itype with the IType (a Java model element)
   corresponding to this ITypeBinding ?tbinding.
   Nil for top-level types."  
  [?tbinding ?itype]
   (l/all 
     (l/!= nil ?itype)
     (el/equals ?itype (.getJavaElement ^ITypeBinding ?tbinding))))

(defn 
  is-typebinding-from-source?
  "Non-relational. Verifies that ITypeBinding ?binding originates 
   from a source file."
  [?binding]
  (l/all 
    (el/equals true (.isFromSource ^ITypeBinding ?binding))))

(defn 
  is-typebinding-from-binary? 
  "Non-relational. Verifies that ITypeBinding ?binding originates 
   from a binary file."
  [?binding]
  (l/all 
    (el/equals false (.isFromSource ^ITypeBinding ?binding))))

(defn
  is-typebinding-primitive?
  "Non-relational. Verifies that ITypeBinding ?binding represents 
   a primitive type."
  [?binding]
  (l/all
    (el/equals true (.isPrimitive ^ITypeBinding ?binding))))

(defn 
  is-typebinding-reference? 
  "Non-relational. Verifies that ITypeBinding ?binding represents
   a reference type."
  [?binding]
  (l/all
    (el/equals false (.isPrimitive ^ITypeBinding ?binding))))

(defn 
  methodbinding-typebinding|declaring
  "Non-relational. Unifies ?tbinding with the ITypeBinding that
   declares IMethodBinding ?mbinding."
  [?mbinding ?tbinding]
  (l/all
    (l/!= ?tbinding nil)
    (el/equals ?tbinding (.getDeclaringClass ^IMethodBinding ?mbinding))))

(defn 
  methodbinding-packagebinding
  "Non-relational. Unifies ?pbinding with the IPackageBinding in
   which IMethodBinding ?mbinding is declared."
  [?mbinding ?pbinding]
  (l/fresh [?tbinding]
         (methodbinding-typebinding|declaring ?mbinding ?tbinding)
         (typebinding-packagebinding ?tbinding ?pbinding)))

(defn 
  is-methodbinding-from-source?
  "Non-relational. Verifies whether IMethodBinding ?binding 
   originates from a source file."
  [?binding]
  (l/fresh [?typebinding]
         (methodbinding-typebinding|declaring ?binding ?typebinding)
         (is-typebinding-from-source? ?typebinding)))
                       
(defn 
  is-methodbinding-from-binary?
  "Non-relational. Verifies whether IMethodBinding ?binding
   originates from a binary file."
  [?binding]
  (l/fresh [?typebinding]
         (methodbinding-typebinding|declaring ?binding ?typebinding)
         (is-typebinding-from-binary? ?typebinding)))

(defn 
  methodbinding-signature
  "Non-relational. Unifies ?sig with the signature string
   of IMethodBinding ?binding."
  [?binding ?sig]
  (l/all
    (el/equals ?sig (damp.ekeko.JavaProjectModel/sootSignatureForMethodBinding ?binding))))

(defn 
  variablebinding|field-typebinding|declaring
  "Non-relational. Unifies ?tbinding with the ITypeBinding that 
   declares field IVariableBinding ?fvbinding."
  [?fvbinding ?tbinding]
   (l/all
     (l/!= nil ?tbinding) 
     (el/equals ?tbinding (.getDeclaringClass ^IVariableBinding ?fvbinding))))

(defn 
  variablebinding|field-signature 
  "Non-relational. Unifies ?sig with the signature string of
   field IVariableBinding ?fvbinding."
  [?fvbinding ?sig]
   (l/all
      (el/equals ?sig (damp.ekeko.JavaProjectModel/sootSignatureForFieldVariableBinding ?fvbinding))))


