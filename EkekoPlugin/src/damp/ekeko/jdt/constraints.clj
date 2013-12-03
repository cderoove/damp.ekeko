(ns damp.ekeko.jdt.constraints
  (:require [clojure.core.logic.fd :as fd])
  (:require [clojure.core.logic :as l])
  (:require [clojure.core.logic.protocols :as prot])
  (:require [clojure.set :as set])
  (:require [damp.ekeko.jdt.reification :as reification])
  (:import [changenodes.comparing.BreadthFirstNodeIterator]))

;;Constraints for Tree Structures
(defprotocol IChildable
  (-child [parentdom childdom])
  (-parent [childdom parentdom]))

(defn parent? [parent child]
  (if (nil? child)
    false
    (let [new-parent (.getParent child)]
      (if (= new-parent parent)
        true
        (recur parent new-parent)))))


(deftype ASTDomain [nodes]
  
  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (case k
      :nodes nodes
      not-found))
  
  fd/ISet
  (-member? [this n]
    (do
      (println "hallokes")
      (contains? nodes n)))

  (-disjoint? [this that]
    (empty? (set/intersection nodes (:s that))))
  
  (-intersection [this that]
    (ASTDomain. (set/intersection nodes (:nodes that))))
    
  (-difference [this that]
    (ASTDomain. (set/difference nodes (:nodes that))))
  
  prot/IMemberCount
  (-member-count [this] 
    (count nodes))
  
  prot/IMergeDomains
  (-merge-doms [this that]
    ;;just calling -intersection does not work for some reason :/
    (ASTDomain. (set/intersection nodes (:nodes that))))
  
  fd/IIntervals
  (-intervals [_] (seq nodes))
  
  IChildable
  (-child [this that] ;;this must be a child of that
    (let [other-nodes (:nodes that)]
      (ASTDomain.
        (set
          (filter 
            (fn [child]
              (some (fn [parent] (parent? parent child)) other-nodes))
            nodes)))))
  (-parent [this that] ;;this must be a parent of that
    (let [other-nodes (:nodes that)]
      (ASTDomain.
        (set
          (filter 
            (fn [parent]
              (some (fn [child] (parent? parent child)) other-nodes))
            nodes))))))


(extend-protocol prot/IForceAnswerTerm
  ASTDomain
  (-force-ans [v x]
    ((fd/map-sum (fn [n] (l/ext-run-csg x n))) (seq (:nodes v)))))


(defn node->astdomain [node]
  "Converts a node to an astdomain by enumerating all its subnodes"
  (let [iterator (new changenodes.comparing.BreadthFirstNodeIterator node)
        seq (iterator-seq iterator)]
    (ASTDomain. (set seq))))


(defn -childc [p c]
  (reify
    clojure.core.logic.protocols.IEnforceableConstraint
    prot/IConstraintStep
    (-step [this s]
      (l/let-dom s [p dp c dc]
        (reify
          clojure.lang.IFn
          (invoke [_ s]
           ((l/composeg
              (fd/process-dom p (-parent dp dc) dp)
              (fd/process-dom c (-child dc dp) dc)) s))
          prot/IEntailed
          (-entailed? [_]
            (and dp dp))
          prot/IRunnable
          (-runnable? [_]
            (and dp dc)))))
   prot/IConstraintOp
   (-rator [_] `childc)
   (-rands [_] [p c])
   prot/IConstraintWatchedStores
   (-watched-stores [this]
     #{::l/subst ::l/fd})))

(defn childc [p c]
  (l/cgoal (-childc p c)))



