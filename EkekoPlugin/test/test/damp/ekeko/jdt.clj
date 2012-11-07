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
  reification-test
  (is 
    (reduce
      (fn [sofar
           [cu key child shouldbecu shouldbekey]]
        (and sofar 
             (identical? cu shouldbecu)
             (identical? key shouldbekey)))
      true
      (damp.ekeko/ekeko [?cu ?key ?child ?shouldbecu ?shouldbekey] 
                        (reification/ast :CompilationUnit ?cu) 
                        (reification/has ?key ?cu ?child) 
                        (reification/has ?shouldbekey ?shouldbecu ?child)))))

;; Test suite

(deftest
   test-suite 
   (test/against-project-named "TestCase-JDT-CompositeVisitor" false reification-test))

(defn 
  test-ns-hook 
  [] 
  (test/with-ekeko-disabled test-suite))


(comment  
  
  (run-tests)
  
  )
  

