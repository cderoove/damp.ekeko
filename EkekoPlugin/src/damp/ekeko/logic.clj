(ns 
  ^{:doc "Auxiliary logic goals, most of which are currently non-relational."
    :author "Coen De Roover"}
   damp.ekeko.logic 
  (:refer-clojure :exclude [==])
   (:use [clojure.core.logic]))

(defn- 
  ekeko-lvar-sym?
  [s]
  (= (first (str s)) \?))

(defn
  ekeko-extract-vars
  [p]
  (set (cond
         (ekeko-lvar-sym? p) [p]
         (coll? p) (filter ekeko-lvar-sym? (flatten p))
         :else nil)))

;;TODO: move to a project with multiple, regular core.logic logic variables
;;TODO: move to constraints such that the evaluating of the expression can be delayed until all variables are bound
(defmacro
  equals
  "Non-relational. Projects all logic variables on the right-hand side
   that start with a question mark (e.g., ?x), evaluates the resulting
   Clojure expression and unifies the result with the left hand-side. "
  [lvar exp & rest]
  (let [expvars#  (ekeko-extract-vars exp)] 
    `(project [~@expvars#]
      (== ~lvar ~exp)
      ~@rest)))

(defmacro 
  perform 
  "Non-relational. Projects all logic variables on the right-hand side
   that start with a question mark (e.g., ?x) and evaluates the resulting
   Clojure expression.

   See also: 
   equals/2 if the value of the expression is needed.
   suceeds/2 if the expressions should evaluate to true."
  [exp]
  `(fresh [returnv#]
          (equals returnv# ~exp)))

(defmacro 
  succeeds
   "Non-relational. Projects all logic variables on the right-hand side
   that start with a question mark (e.g., ?x) and verifies that 
   the resulting Clojure expression evaluates to true."
  [exp]
  `(equals true ~exp))

(defmacro 
  equals-without-exception 
  "Like equals/2, but catches Exception thrown by exp.
   Fails if an exception is thrown."
  [lvar exp]
  `(fresh [e#]
     (!= ~lvar :ekeko_exception_occurred)
     (equals ~lvar (try ~exp (catch Exception e# 
                               (do 
                                 (println e#)
                                 :ekeko_exception_occurred))))))

(defmacro 
  succeeds-without-exception
  "Like succeeds/2, but catches Exception thrown by exp.
   Fails if an exception is thrown."
  [exp]
  `(equals-without-exception true ~exp))


;(defmacro 
;  is-false? 
;  [exp]
;  `(== false ~exp))

(defmacro
  contains
  "Relation between a collection (seq c) and one of its elements e."
  [c e]
  `(fresh [s#]
     (equals s# (seq ~c))
     (membero ~e s#)))

(defmacro
  v+ 
  "Non-relational. Verifies that logic variable v is ground."
  [v]
  `(project [~v] (succeeds (not (lvar? ~v)))))

(defmacro
  v-
  "Non-relational. Verifies that logic variable v is not ground."
  [v]
  `(project [~v] (succeeds (lvar? ~v))))


(defmacro
  fails
  "Succeeds if argument fails. Implements negation as failure." 
  [goal]
  `(condu
     [~goal fail]
     [succeed]))

(defn 
  one
  "If g succeeds, succeeds once." 
  [g]
  (condu
    [g succeed]))

