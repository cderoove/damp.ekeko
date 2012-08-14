(ns test.damp.ekeko
  (:refer-clojure :exclude [==])
  (:use [clojure.core.logic :exclude [is]] :reload)
  (:use [damp.ekeko])
  (:use [damp.ekeko.jdt reification basic])
  (:use clojure.test))

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
      (ekeko [?cu ?key ?child ?shouldbecu ?shouldbekey] 
                    (ast :CompilationUnit ?cu) 
                    (has ?key ?cu ?child) 
                    (has ?shouldbekey ?shouldbecu ?child)))))
               
;(run-tests)