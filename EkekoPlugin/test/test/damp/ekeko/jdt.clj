(ns 
  ^{:doc "Test suite for Ekeko JDT relations."
    :author "Coen De Roover"}
  test.damp.ekeko.jdt
  (:refer-clojure :exclude [== type declare])
  (:use [clojure.core.logic :exclude [is]] :reload)
  (:use [damp.ekeko logic])
  (:use clojure.test)
  (:require [test.damp [ekeko :as test]]
            [damp.ekeko.jdt [reification :as reification]]))

;; Tests

(deftest
  ;TODO: might be nice to have it work for other values (and nil) as well,
  ;possible if reification of primitive values (and nil) is changed ... but comes at a high performance cost
  ;(especially when nil has to be wrapped)
  reification-has-relational
  (doseq [[node property child shouldbenode shouldbeproperty]  
          (damp.ekeko/ekeko [?node ?property ?child ?shouldbenode ?shouldbeproperty] 
                  (fresh [?kind]
                         (reification/ast ?kind ?node) 
                         (reification/has ?property ?node ?child) 
                         (conde [(succeeds (instance? org.eclipse.jdt.core.dom.ASTNode ?child))]
                                [(succeeds (instance? java.util.List ?child))]) 
                         (reification/has ?shouldbeproperty ?shouldbenode ?child)))]
    (is (identical? node shouldbenode)
    (is (identical? property shouldbeproperty)))))


(deftest
  ^{:doc "Tests whether ast/2 quantifies over each node encountered during 
          a recursive descent of all compilation units."}
   reification-ast-traversal
  (test/tuples-are 
    (damp.ekeko/ekeko [?ast]
                      (fresh [?kind]
                             (reification/ast ?kind ?ast)))
    (damp.ekeko/ekeko [?ast]
                      (fresh [?cu]
                             (reification/ast :CompilationUnit ?cu)
                             (conde [(== ?cu ?ast)]
                                    [(reification/child+ ?cu ?ast)])))))

;; Test suite


(deftest
  test-suite
  (let [visitorproject "TestCase-JDT-CompositeVisitor"]
    (test/against-project-named visitorproject false reification-has-relational)
    (test/against-project-named visitorproject false reification-ast-traversal)))
  
(defn 
  test-ns-hook 
  [] 
  (test/with-ekeko-disabled test-suite))


(comment  
  
  (run-tests)
  
  )
  

