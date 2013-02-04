(ns 
  ^{:doc "Test suite for Ekeko JDT relations."
    :author "Coen De Roover"}
  test.damp.ekeko.jdt
  (:refer-clojure :exclude [== type declare])
  (:require 
    [clojure.core.logic :exclude [is] :as l]
    [test.damp [ekeko :as test]]
    [damp.ekeko [logic :as logic]]
    [damp.ekeko.jdt 
     [astnode :as astnode]
     [reification :as reification]
     [basic :as basic]])
  (:use clojure.test))


;; AST Tests

(deftest
  reification-ast
  ^{:doc "Tests relational nature of the second argument of ast/2."}
  (doseq [kind astnode/ekeko-keywords-for-ast-classes]
    (test/tuples-are
      (damp.ekeko/ekeko [?ast]
             (reification/ast kind ?ast))
      (damp.ekeko/ekeko [?ast]
             (reification/ast kind ?ast) 
             (reification/ast kind ?ast)))))
  

(deftest
  ^{:doc "Tests whether has/3 is relational."}
  reification-has-relational
  (doseq [[node property child shouldbenode shouldbeproperty]  
          (damp.ekeko/ekeko [?node ?property ?child ?shouldbenode ?shouldbeproperty] 
                  (l/fresh [?kind]
                         (reification/ast ?kind ?node) 
                         (reification/has ?property ?node ?child) 
                         (reification/has ?shouldbeproperty ?shouldbenode ?child)))]
    (is (identical? node shouldbenode)
    (is (identical? property shouldbeproperty)))))


(deftest
  ^{:doc "Tests whether ast/2 quantifies over each node encountered during 
          a recursive descent of all compilation units."}
   reification-ast-child+
  (test/tuples-are 
    (damp.ekeko/ekeko [?ast]
                      (l/fresh [?kind]
                               (reification/ast ?kind ?ast)))
    (damp.ekeko/ekeko [?ast]
                      (l/fresh [?cu]
                               (reification/ast :CompilationUnit ?cu)
                               (l/conde 
                                 [(clojure.core.logic/== ?cu ?ast)]
                                 [(reification/child+ ?cu ?ast)])))))

(deftest
  reification-value
  ^{:doc "Tests relational nature of value/1, 
          listvalue/1, nullvalue/1, primitivevalue/1."}
  (doseq [valpred [reification/value reification/listvalue
                   reification/nullvalue reification/primitivevalue]]
    (test/tuples-are
      (damp.ekeko/ekeko [?val]
                        (valpred ?val))
      (damp.ekeko/ekeko [?val]
                        (valpred ?val) 
                        (valpred ?val)))))
  


(deftest 
  reification-child-parent 
  ^{:doc "Tests relation between child/3 and parent/2."}
  (doseq 
    [[node child parent]
     (damp.ekeko/ekeko [?node ?child ?parent] 
                       (l/fresh [?kind ?property]
                              (reification/ast ?kind ?node)
                              (reification/child ?property ?node ?child)
                              (reification/ast-parent ?child ?parent)))]
    (is (identical? node parent))))


(deftest
  reification-ast-specialkinds
  ^{:doc "Tests relational nature of ast/resolvable/2"}
  (doseq [valpred [reification/ast-resolveable 
                   reification/ast-declaration
                   reification/ast-fieldaccess
                   reification/ast-type
                   reification/ast-resolveable-declaration
                   reification/ast-invocation
                   reification/ast-expression-reference
                   reification/ast-name-of-parameter
                   ]]
    (test/tuples-are
      (damp.ekeko/ekeko [?val]
                        (l/fresh [?kind]
                               (valpred ?kind ?val)))
      (damp.ekeko/ekeko [?val]
                        (l/fresh [?kind]
                               (valpred ?kind ?val) 
                               (valpred ?kind ?val))))))

(deftest
  reification-ast-subnodekinds
  ^{:doc "Tests whether ast/2 supports AST nodes of non-leaf kinds (non-exhaustive)."}
  (doseq [[subkind kind]
          [[:SingleVariableDeclaration :VariableDeclaration]
           [:VariableDeclarationFragment :VariableDeclaration]
           [:ReturnStatement :Statement]
           [:SimpleType :Type]]]
    (test/tuples-aresubset
      (damp.ekeko/ekeko [?ast] (reification/ast subkind ?ast))
      (damp.ekeko/ekeko [?ast] (reification/ast kind ?ast)))))
  



;; IBinding tests


(deftest 
  reification-ast-bindings
  ^{:doc "Simply tests whether binding-related ternary predicates execute without exception-free."}
  (doseq [bindingpred [reification/ast-expression-typebinding
                       reification/ast-type-binding
                       reification/ast-fieldaccess-binding
                       reification/ast-invocation-binding
                       reification/ast-typedeclaration-binding
                       reification/ast-declares-binding
                       ]]
    (doseq [[binding] (damp.ekeko/ekeko [?binding]
                                      (l/fresh [?kind ?ast]
                                             (bindingpred ?kind ?ast ?binding)))]
      (is (instance? org.eclipse.jdt.core.dom.IBinding binding)))))



;; Java Model Tests

(deftest
  reification-modelunaries
  ^{:doc "Tests that unary Java model reification predicates do not throw 
          an exception (testing whether they are relational takes a long time)."}
  (doseq [modelpred [reification/packagefragmentroot
                     reification/packagefragmentroot-from-binary
                     reification/packagefragmentroot-from-source
                     reification/packagefragment
                     reification/classfile
                     reification/compilationunit
                     reification/type
                     reification/type-from-binary
                     reification/type-from-source
                     reification/initializer
                     reification/method
                     reification/method-from-source
                     reification/method-from-binary
                     reification/field
                     reification/field-from-source
                     reification/field-from-binary
                     ]]
    (damp.ekeko/ekeko [?t] (modelpred ?t))))


(deftest 
  reification-modelelement
  ^{:doc "Tests relational nature of element/1."}
  (test/tuples-are 
    (damp.ekeko/ekeko [?k ?e] 
                      (reification/element ?k ?e))
    (damp.ekeko/ekeko [?k ?e] 
                      (reification/element ?k ?e)
                      (reification/element ?k ?e))))
       

;; Basic relations

(deftest
  basic-ast-location
  ^{:doc "Superficial test for ast-location/2."}
  (is (< 0 (count (damp.ekeko/ekeko [?ast ?locv] (basic/ast-location ?ast ?locv))))))

(deftest
  basic-ast-encompassing-typedeclaration
  ^{:doc "Superficial test for ast-encompassing-typedeclaration/2."}
  (is (= 285 (count (damp.ekeko/ekeko [?ast ?typedec] (basic/ast-encompassing-typedeclaration ?ast ?typedec))))))

(deftest
  basic-ast-encompassing-methoddeclaration
  ^{:doc "Superficial test for ast-encompassing-method/2."}
  (is (= 240 (count (damp.ekeko/ekeko [?ast ?methoddec] (basic/ast-encompassing-method ?ast ?methoddec))))))

    
(deftest
  basic-modifiers
  ^{:doc "Superficial test for modifier-*/1."}
  (is 
    (= [2 32 0 0 0 0 0 0 0 0 0]
       (for [modpred [basic/modifier-static basic/modifier-public basic/modifier-protected basic/modifier-private 
                      basic/modifier-abstract basic/modifier-final basic/modifier-native basic/modifier-synchronized 
                      basic/modifier-transient basic/modifier-volatile basic/modifier-strictfp]]
         (count (damp.ekeko/ekeko [?mod] (modpred ?mod)))))))

(deftest
  basic-ast-declaration-modifier 
  ^{:doc "Superficial test for ast-declaration-modifier/3."}
  (is (= 34 (count (damp.ekeko/ekeko [?key ?ast ?mod] (basic/ast-declaration-modifier ?key ?ast ?mod))))))
  

  

;; Test suite


(deftest
  test-suite
  (let [visitorproject "TestCase-JDT-CompositeVisitor"]
    
    (comment
    
    ;Reification
    ;-----------
    
    ;AST    
    (test/against-project-named visitorproject false reification-ast)
    (test/against-project-named visitorproject false reification-has-relational)
    (test/against-project-named visitorproject false reification-ast-child+)
    (test/against-project-named visitorproject false reification-value)
    (test/against-project-named visitorproject false reification-child-parent)
    (test/against-project-named visitorproject false reification-ast-specialkinds)
    (test/against-project-named visitorproject false reification-ast-subnodekinds)
    
    ;IBinding
    (test/against-project-named visitorproject false reification-ast-bindings)
    
    
    ;Model
    (test/against-project-named visitorproject false reification-modelunaries)
    
    
    ;http://dev.clojure.org/jira/browse/LOGIC-111
    ;(test/against-project-named visitorproject false reification-modelelement)
    
    
    )
    
    ;Basic
    ;-----

    (test/against-project-named visitorproject false basic-ast-location)
    (test/against-project-named visitorproject false basic-ast-encompassing-typedeclaration)
    (test/against-project-named visitorproject false basic-ast-encompassing-methoddeclaration)
    (test/against-project-named visitorproject false basic-modifiers)
    (test/against-project-named visitorproject false basic-ast-declaration-modifier)

   
    
    ))
  
(defn 
  test-ns-hook 
  [] 
  (test/with-ekeko-disabled test-suite))


(comment  
  
  (run-tests)
  
  )


  
