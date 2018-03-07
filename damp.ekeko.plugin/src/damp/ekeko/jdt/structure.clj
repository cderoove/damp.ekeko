(ns
  ^{:doc "Relations reifiying the structural relations of the JDT Model."
    :author "Coen De Roover"}
  damp.ekeko.jdt.structure
  (:refer-clojure :exclude [== type])
  (:require [clojure.core [logic :as l]])
  (:require 
    [damp.ekeko
     [logic :as el] [ekekomodel :as ekekomodel]]
    [damp.ekeko.jdt 
     [javaprojectmodel :as javaprojectmodel]
     [astnode :as astnode]])
  (:import 
    [org.eclipse.core.runtime IProgressMonitor]
    [org.eclipse.jdt.core IJavaElement ITypeHierarchy IType IPackageFragment IClassFile ICompilationUnit
     IJavaProject WorkingCopyOwner IMethod IPackageFragmentRoot ITypeParameter IInitializer
    IImportDeclaration IPackageDeclaration ILocalVariable IAnnotation]  
    [org.eclipse.jdt.core.dom TypeDeclaration Name FieldAccess SuperFieldAccess ClassInstanceCreation ConstructorInvocation SuperConstructorInvocation SuperMethodInvocation MethodInvocation QualifiedName  SimpleName Type CompilationUnit ASTNode ASTNode$NodeList ImportDeclaration Modifier IPackageBinding ITypeBinding IVariableBinding IMethodBinding IAnnotationBinding IMemberValuePairBinding]))

 


(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IJavaElement)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IType)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.ITypeParameter)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IInitializer)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IAnnotation)

(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.ICompilationUnit)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IClassFile)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IOrdinaryClassFile)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IPackageFragment)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IPackageFragmentRoot)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IJavaProject)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IMethod)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.ILocalVariable)

(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IField)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IImportDeclaration)
(el/extend-ISupportContains-to-arrays-of-class org.eclipse.jdt.core.IPackageDeclaration)


;; Java Model Reification
;; ----------------------

(defn
  packagefragment|root
  "Relation of IPackageFragmentRoot instances ?r."
  [?r]
  (let [roots (mapcat javaprojectmodel/javaproject-packagefragmentroots
                      (javaprojectmodel/ekeko-javaprojects))]
    (l/all
      (el/contains  roots ?r))))
  
(defn 
  packagefragment|root|binary
  "Relation of IPackageFragmentRoot instances ?r that originate from byte code."
  [?r]
  (l/all
    (packagefragment|root ?r)
    (el/succeeds (javaprojectmodel/packagefragmentroot-binary? ?r))))


(defn 
   packagefragment|root|source
  "Relation of IPackageFragmentRoot instances ?r that originate from source code."
  [?r]
  (l/all
    (packagefragment|root ?r)
    (el/succeeds (javaprojectmodel/packagefragmentroot-source? ?r))))


(defn
  packagefragment|root-packagefragment
  "Relation between a IPackageFragmentRoot ?r and one of its IPckageFragment instances ?p." 
  [?r ?f]
  (l/fresh [?fragments] 
    (packagefragment|root ?r)
    (el/equals ?fragments (javaprojectmodel/packagefragmentroot-fragments ?r))
    (el/contains ?fragments ?f)))


(defn
  packagefragment 
  "Relation of IPackageFragment instances."
  [?f]
  (l/fresh [?r]
         (packagefragment|root-packagefragment ?r ?f)))

(defn
  packagefragment-name|string
  "Relation between an IPackageFragment ?f and its name String ?n." 
  [?f ?n]
  (l/fresh [?r]
         (packagefragment|root-packagefragment ?r ?f)
         (el/equals ?n (javaprojectmodel/packagefragment-name ?f))))
  

(defn 
  packagefragment-classfile
  "Relation between an IPackageFragment ?f and one of its IClassFile instances ?c."
  [?f ?c]
  (l/fresh [?files]
    (packagefragment ?f)
    (el/equals ?files (.getClassFiles ^IPackageFragment ?f))
    (el/contains ?files ?c)))

(defn 
  classfile
  "Relation of IClassFile instances ?c."
  [?c]
  (l/fresh [?p]
         (packagefragment-classfile ?p ?c)))

(defn 
  packagefragment-compilationunit
  "Relation between an IPackageFragment ?f and one of its ICompilationUnit instances ?c."
  [?f ?c]
  (l/fresh [?cus]
    (packagefragment ?f)
    (el/equals ?cus (.getCompilationUnits ^IPackageFragment ?f))
    (el/contains ?cus ?c)))

(defn 
  compilationunit
  "Relation of ICompilationUnit instances ?c"
  [?c]
  (l/fresh [?p]
         (packagefragment-compilationunit ?p ?c)))

(defn 
  classfile-type
  "Relation between an IClassFile ?c and the IType ?t it declares."
  [?c ?t]
  (l/all
    (classfile ?c)
    (el/equals ?t (.getType ^IClassFile ?c))))

(defn 
  compilationunit-type
  "Relation between an ICompilationUnit ?c and one of the top-level IType instances ?t it declares."
  [?c ?t]
  (l/fresh [?types]
    (compilationunit ?c)
    (el/equals ?types (.getTypes ^ICompilationUnit ?c))
    (el/contains ?types ?t)))



(declare type-membertype)

(defn
  type
  "Relation of IType instances ?t." 
  [?t]
  (l/conda [(el/v+ ?t) 
            (el/succeeds (instance? IType ?t))]
           [(el/v- ?t) 
            (l/conde
              [(l/fresh [?classfile]
                        (classfile-type ?classfile ?t))]
              [(l/fresh [?compilationunit ?toplevelt]
                        (compilationunit-type ?compilationunit ?toplevelt)
                        (l/conde [(l/== ?toplevelt ?t)]
                                 [(type-membertype ?toplevelt ?t)]))])]))


(defn
  type|source
  "Relation of IType instances ?t that originate from a source file."
  [?t]
  (l/all 
    (type ?t)
    (el/equals false (.isBinary ^IType ?t))))

(defn
  type|binary
  "Relation of IType instances ?t that originate from a binary file."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isBinary ^IType ?t))))


(defn
  type-membertype
  "Relation of IType ?t and one of its immediate member types ?mt."
  [?t ?mt]
  (l/fresh [?types]
    (type ?t)
    (el/equals ?types  (.getTypes ^IType ?t))
    (el/contains ?types ?mt)))

(defn
  type-name|simple|string
  "Relation of IType ?t and its simple name String ?n."
  [?t ?n]
  (l/all
    (type ?t)
    (el/equals ?n (.getElementName ^IType ?t))))



;;geeft unresolved types terug, andere zijn wel resolved
;;zullen niet unificeren!
(defn-
  auxfor-type-name|qualified|string 
  [n]
  (into []
        (into #{}
              (map 
                (fn [p]
                  (let [^WorkingCopyOwner wco nil
                        ^IProgressMonitor pm nil]
                    (.findType ^IJavaProject p ^String n wco pm)))
                (javaprojectmodel/ekeko-javaprojects)))))
  

(defn
  type-name|qualified|string
  "Relation of IType ?t and its fully qualified name String ?n."
  [?t ?n]
  (l/conda 
    [(el/v+ ?t)
     (el/succeeds (instance? IType ?t))
     (el/equals ?n (.getFullyQualifiedName ^IType ?t))]
    [(el/v- ?t)
     (type ?t)
     (type-name|qualified|string ?t ?n)]))
     
         
(defn
  type-initializer
  "Relation between an IType ?t and one of its IInitializer initializers ?i.

   Note that their are none for binary types."
  [?t ?i]
  (l/fresh [?inits]
    (type ?t)
    (el/equals ?inits  (.getInitializers ^IType ?t))
    (el/contains ?inits ?t)))


(defn
  type-method
  "Relation between IType ?t and one if its declared IMethod instances ?m."
  [?t ?m]
  (l/fresh [?ms]
    (type ?t)
    (el/equals ?ms (.getMethods ^IType ?t))
    (el/contains ?ms ?m)))

(defn 
  method
  "Relation of IMethod instances.

  See also: 
  method-from-source/1 and method-from-binary/1."
  [?m]
  (l/fresh [?t]
         (type-method ?t ?m)))

(defn
  method|source
  "Relation of IMethod instances declared in a source type.

  Note that (ast :MethodDeclaration ?m) is much more efficient
  and corresponds to the relation of method ASTs."
  [?m]
  (l/fresh [?t]
         (type|source ?t)
         (type-method ?t ?m)))

(defn 
  method|binary
  "Relation of IMethod instances declared in a binary type."
  [?m]
  (l/fresh [?t]
         (type|binary ?t)
         (type-method ?t ?m)))

(defn
  type-field
  "Relation between IType ?t and one if its declared IField instances ?f."
  [?t ?f]
  (l/fresh [?fs]
    (type ?t)
    (el/equals ?fs (.getFields ^IType ?t))
    (el/contains ?fs ?f)))

(defn 
  field
  "Relation of IField instances.

  See also: 
  field-from-source/1 and field-from-binary/1."
  [?f]
  (l/fresh [?t]
         (type-field ?t ?f)))

(defn
  field|source
  "Relation of IField instances declared in a source type."
  [?f]
  (l/fresh [?t]
         (type|source ?t)
         (type-field ?t ?f)))

(defn 
  field|binary
  "Relation of IField instances declared in a binary type."
  [?f]
  (l/fresh [?t]
         (type|binary ?t)
         (type-field ?t ?f)))

(defn 
  element
  "Relation of IJavaElement instances ?element and the keyword ?key representing their kind."
  [?key ?element]
  (l/conda
    [(el/v+ ?element)
     (el/succeeds (instance? IJavaElement ?element))
     (el/equals ?key (astnode/ekeko-keyword-for-class-of ?element))]
    [(el/v- ?element)
     (l/conda
       [(l/== ?key :PackageFragmentRoot)
        (packagefragment|root ?element)]
       [(l/== ?key :PackageFragment)
        (l/fresh [?root]
               (packagefragment|root ?root)
               (packagefragment|root-packagefragment ?root ?element))]
       [(l/== ?key :CompilationUnit)
        (compilationunit ?element)]
        [(l/== ?key :ClassFile)
         (classfile ?element)]
        [(l/== ?key :Type)
         (type ?element)]
 ;       [(l/== ?key :Initializer)
 ;        (initializer ?element)]
        [(l/== ?key :Method)
         (method ?element)]
        [(l/== ?key :Field)
         (field ?element)]
        )]))
       
(defn
  type-typeparameters 
  "Relation between an IType ?t and its formal type parameters ITypeParameter[] ?ps."
  [?t ?ps]
  (l/all
    (type ?t)
    (el/equals ?ps (.getTypeParameters ^IType ?t))))

(defn
  method-typeparameters
  "Relation between an IMethod ?m and its formal type parameters ITypeParameter[] ?ps."
  [?m ?ps]
  (l/all
    (method ?m)
    (el/equals ?ps (.getTypeParameters ^IMethod ?m))))

(defn
  type|annotation
  "Relation of IType instances ?t that represent an annotation type."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isAnnotation ^IType ?t))))

(defn
  type|anonymous
  "Relation of IType instances ?t that represent an anonymous type."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isAnonymous ^IType ?t))))

(defn
  type|class
  "Relation of IType instances ?t that represent a class."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isClass ^IType ?t))))

(defn
  type|enum
  "Relation of IType instances ?t that represent an enumeration class."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isEnum ^IType ?t))))

(defn
  type|interface
  "Relation of IType instances ?t that represent an interface."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isInterface ^IType ?t))))

(defn
  type|local
  "Relation of IType instances ?t that represent a local type."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isLocal ^IType ?t))))

(defn
  type|member
  "Relation of IType instances ?t that represent a member type."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isMember ^IType ?t))))

(defn
  type|resolved
  "Relation of IType instances ?t that represent a resolved type."
  [?t]
  (l/all 
    (type ?t)
    (el/succeeds (.isResolved ^IType ?t))))


(defn- 
  element-ekeko-model 
  [?element ?ekeko-model]
  (l/all
    (el/equals ?ekeko-model (.getJavaProjectModel 
                           ^damp.ekeko.EkekoModel
                           (ekekomodel/ekeko-model) 
                           (.getJavaProject ^IJavaElement ?element)))))

;NOTE: tabling cannot be used here because typehierarchies are static 
;the ekeko model installs a listener for changes in the typehierarchy
(defn-
  itype-supertypehierarchy ;TODO: same for complete hierarchy, but those are presumably more expensive to construct and maintain
  [?itype ?typeHierarchy]
  (l/all
    (el/equals ?typeHierarchy (.getTypeHierarchy ^damp.ekeko.EkekoModel (ekekomodel/ekeko-model) ?itype))))


;Note that the JDT does not consider java.lang.Object to be a supertype of any interface type.
;Returns all resolved supertypes of the given type, in bottom-up order. 
(defn-
  itype-super-itypes
  [?itype ?supers]
  (l/fresh [?hierarchy]
         (itype-supertypehierarchy ?itype ?hierarchy)
         (l/!= nil ?hierarchy)
         (el/equals ?supers (.getAllSupertypes ^ITypeHierarchy ?hierarchy ?itype))))

(defn-
  itype-sub-itypes
  [?itype ?subs]
  (l/fresh [?hierarchy]
         (itype-supertypehierarchy ?itype ?hierarchy)
         (el/equals ?subs (.getAllSubtypes ^ITypeHierarchy ?hierarchy ?itype))))

(declare type-type|sub+)

(defn 
  type-type|super+
  "Successively unifies ?super-itype with every 
   supertype of the given IType ?itype, in bottom-up order. 

   Note that the JDT does not consider java.lang.Object
   to be a supertype of any interface type."
  [?itype ?super-itype]
  (l/conda [(el/v+ ?itype)
            (type ?itype)
            (l/fresh [?supers]
                     (l/!= nil ?super-itype)
                     (itype-super-itypes ?itype ?supers)
                     (el/contains  ?supers ?super-itype))]
           [(el/v- ?itype)
            (type-type|sub+ ?super-itype ?itype)]))

(defn 
  type-type|sub+
  "Successively unifies ?sub-itype with every 
   subtype of the given IType ?itype. 

   Note that the JDT does not consider java.lang.Object
   to be a supertype of any interface type."
  [?itype ?sub-itype]
  (l/fresh [?subs]
           (type ?itype)
           (l/!= nil ?sub-itype)
           (itype-sub-itypes ?itype ?subs)
           (el/contains  ?subs ?sub-itype)))


  
