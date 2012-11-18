(ns 
  ^{:doc "Test suite for Ekeko JDT relations."
    :author "Coen De Roover"}
  test.damp.ekeko.jdt
  (:refer-clojure :exclude [== type declare])
  (:use [clojure.core.logic :exclude [is]] :reload)
  (:use [damp.ekeko logic])
  (:use clojure.test)
  (:require [test.damp [ekeko :as test]]
            [damp.ekeko.jdt 
             [astnode :as astnode]
             [reification :as reification]]))

;; Tests

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
                  (fresh [?kind]
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
                      (fresh [?kind]
                             (reification/ast ?kind ?ast)))
    (damp.ekeko/ekeko [?ast]
                      (fresh [?cu]
                             (reification/ast :CompilationUnit ?cu)
                             (conde [(== ?cu ?ast)]
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
  
  

;; Test suite


(deftest
  test-suite
  (let [visitorproject "TestCase-JDT-CompositeVisitor"]
      (test/against-project-named visitorproject false reification-ast)
      (test/against-project-named visitorproject false reification-has-relational)
      (test/against-project-named visitorproject false reification-ast-child+)
      (test/against-project-named visitorproject false reification-value)
      
      ))
  
(defn 
  test-ns-hook 
  [] 
  (test/with-ekeko-disabled test-suite))


(comment  
  
  (run-tests)
  
  )
  

