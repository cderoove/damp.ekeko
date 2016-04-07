(ns test.damp.ekeko
  (:refer-clojure :exclude [== type declare])
  (:require
    [clojure.core.logic :exclude [is] :as l]
    [damp.ekeko]
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
    
;(defn
;  against-project
;  [p enable-soot? f]
;  (try
;    (println "Enabling Ekeko nature on project: " p)
;    (ws/workspace-project-enable-ekeko! p)
;    (ws/workspace-wait-for-builds-to-finish)
;    (when 
;      enable-soot?
;      (do
;        (println "Enabling Soot nature on project: " p)
;        (spm/enable-soot-nature! p)))
;    (f)
;    (finally
;      (println "Disabling Ekeko nature for project: " p)
;      (when 
;        enable-soot?
;        (do
;          (println "Disabling Soot nature for project: " p)
;          (spm/disable-soot-nature! p)))
;      (ws/workspace-project-disable-ekeko! p))))

(defn
  against-projects
  [projects enable-soot? f]
  (try
    (doseq [p projects]
      (println "Enabling Ekeko nature on project: " p)
      (ws/workspace-project-enable-ekeko! p)
      (ws/workspace-wait-for-builds-to-finish)
      (when 
        enable-soot?
        (do
          (println "Enabling Soot nature on project: " p)
          (spm/enable-soot-nature! p))))
    (f)
    (finally
      (doseq [p projects]
        (println "Disabling Ekeko nature for project: " p)
        (when 
          enable-soot?
          (do
            (println "Disabling Soot nature for project: " p)
            (spm/disable-soot-nature! p)))
        (ws/workspace-project-disable-ekeko! p)))))

(defn
  against-project
  [p enable-soot? f]
  (against-projects [p] enable-soot? f))

(defn
  against-projects-named
  [names enable-soot? f]
  (println "Testing against projects named " (apply str (interpose ", " names)) ": " f)
  (against-projects 
    (map (fn [n] (ws/workspace-project-named n)) names)
    enable-soot? f))

(defn
  against-project-named
  [n enable-soot? f]
  (println "Testing against project named " n ": " f)
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
  tuples-correspond
  "Verifies whether the stringset representation of its
   first argument (tuples obtained as solutions to an Ekeko query)
   correspond to the string given as its second argument.
   Such a string can be obtained from tuples through tuples-to-stringsetstring."
  [tuples stringsetstring]
  (is 
    (empty?
      (clojure.set/difference
        (tuples-to-stringset tuples)
        (read-string stringsetstring)))))
  
(defn
  tuples-are
  "Verifies whether its argument sequences contain the
   same tuples (obtained as solutions to an Ekeko query)."
  [tuples1 tuples2]
  (is 
    (empty?
      (clojure.set/difference (into #{} tuples1)
                              (into #{} tuples2)))))

(defn
  nonemptytuples-are
  "Verifies whether its argument sequences are not empty,
   and contain the same tuples (obtained as solutions to an Ekeko query)."
  [tuples1 tuples2]
  (is (seq tuples1))
  (is (seq tuples2))
  (is (count tuples1) (count tuples2))
  (tuples-are tuples1 tuples2))

(defn
  tuples-aresubset
  "Verifies whether all elements from its first argument sequence are
   included in its second argument sequence (both obtained as solutions to an Ekeko query)."
  [tuples1 tuples2]
  (is 
    (clojure.set/subset? (into #{} tuples1)
                         (into #{} tuples2))))

  
  

  
       
;; Actual Tests
;; ------------


(deftest
  test-ekeko
  (tuples-correspond
    (damp.ekeko/ekeko [?x ?y] (l/== ?x 1) (l/== ?y 2))
    "#{(\"1\" \"2\")}") ;string obtained through (tuples-to-stringsetstring  (damp.ekeko/ekeko [?x ?y] (== ?x 1) (== ?y 2))) 
  (tuples-are 
    (damp.ekeko/ekeko [?x ?y] (l/== ?x 1) (l/== ?y 2))
    (damp.ekeko/ekeko [?x ?y] (l/== ?x 1) (l/== ?y 2)))) 


;; Test Suite
;; ----------

;note: (run-tests) will report that one more test has been run (successfully)
;than those that are listed here .. seems "normal"


(deftest
   test-suite 
   (test-ekeko))

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