(ns test.damp.ekeko
  (:refer-clojure :exclude [== type declare])
  (:use [clojure.core.logic :exclude [is]] :reload)
  (:require [damp.ekeko])
  (:require
    [damp.ekeko.workspace
     [workspace :as ws]]
    [damp.ekeko.soot
     [projectmodel :as spm]])
  (:use clojure.test))


;see http://richhickey.github.com/clojure/clojure.test-api.html for api
;followed http://twoguysarguing.wordpress.com/2010/03/24/fixies/ to workaround limitations regarding setup/teardown of in


;; Supporting functions
;; --------------------

;; Test setup / teardown

(defn
  with-ekeko-disabled
  [f]
  (println "Disabling Ekeko nature on all projects.")
  (spm/workspace-disable-soot!)
  (ws/workspace-disable-ekeko!)
  (ws/workspace-wait-for-builds-to-finish)
  (f))
    
(defn
  against-project
  [p enable-soot? f]
  (try
    (println "Enabling Ekeko nature on project: " p)
    (ws/workspace-project-enable-ekeko! p)
    (ws/workspace-wait-for-builds-to-finish)
    (when 
      enable-soot?
      (do
        (println "Enabling Soot nature on project: " p)
        (spm/enable-soot-nature! p)))
    (f)
    (finally
      (println "Disabling Ekeko nature for project: " p)
      (when 
        enable-soot?
        (do
          (println "Disabling Soot nature for project: " p)
          (spm/disable-soot-nature! p)))
      (ws/workspace-project-disable-ekeko! p))))

(defn
  against-project-named
  [n enable-soot? f]
  (against-project (ws/workspace-project-named n) enable-soot? f))


;; Query results

(defn
  tuples-to-stringset
  "For a sequence of sequences (e.g., query results), converts each inner sequence 
   element to a string and returns the resulting set of converted inner sequences."
  [seqofseqs]
  (set (map (partial map str) seqofseqs)))

(defn
  tuples-to-stringsetstring
  "For a sequence of sequences (e.g., query results), converts each inner sequence 
   element to a string and returns the string representation of the resulting set of
   converted inner sequences.

   Run this on the results of a working query to transform them to a string that is
   used for the tests. For example:
   (tuples-to-stringsetstring (damp.ekeko/ekeko [?itmethod]
           (assumptions/intertypemethod-unused ?itmethod)))"
  [seqofseqs]
  (str (tuples-to-stringset seqofseqs)))

(defn
  tuples-are
  "Verifies whether the stringset representation of the tuples corresponds to the given string
   (obtained through tuples-to-stringsetstring)."
  [tuples stringsetstring]
  (is 
    (empty?
      (clojure.set/difference
        (tuples-to-stringset tuples)
        (read-string stringsetstring)))))

;; Actual Tests
;; ------------

(deftest
  test-ekeko
  (tuples-are
    (damp.ekeko/ekeko [?x ?y] (== ?x 1) (== ?y 2))
    "#{(\"1\" \"2\")}")) ;string obtained through (tuples-to-stringsetstring  (damp.ekeko/ekeko [?x ?y] (== ?x 1) (== ?y 2))) 


;; Test Suite
;; ----------

;note: (runtests) will report that one more test has been run (successfully)
;than those that are listed here .. seems "normal"

(deftest
   test-suite 
   (test-ekeko)
   )

(defn 
  test-ns-hook 
  [] 
  (with-ekeko-disabled test-suite))


;; Example REPL Session that runs the test
;; ---------------------------------------

; note: uncommenting would run the tests upon loading

(comment  
  
  (run-tests)
  
  )