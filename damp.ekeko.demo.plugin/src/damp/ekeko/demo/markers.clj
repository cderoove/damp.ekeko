(ns
  damp.ekeko.demo.markers
  (:refer-clojure :exclude [== type])
  (:use [clojure.core.logic])
  (:require
    [damp.ekeko]
    [damp.ekeko.demo [relations :as relations]]
    [damp.ekeko [ekekomodel :as ekekomodel]])
  (:import 
    [org.eclipse.jdt.core.dom ASTNode]
    [baristaui.util MarkerUtility]
    [damp.ekeko IEkekoModelUpdateListener EkekoModelRemovedEvent]))

(defn
  model-update-listener
  [execute-upon-change]
  (reify 
    IEkekoModelUpdateListener
    (projectModelUpdated [this model]
                         (when
                           (instance? EkekoModelRemovedEvent model)
                           (do
                             (println "Project model no longer being queried:" model)
                             (.removeListener (ekekomodel/ekeko-model) this)))
                         (execute-upon-change model))))

(defn-
  incremental-node-manager
  [node-retrieval-function
   nodes-added-function 
   nodes-removed-function]
  (let [previous-nodes (atom #{})]
    (fn [event]
      (let [nodes (into #{} (node-retrieval-function))]
        (nodes-removed-function (clojure.set/difference @previous-nodes nodes))
        (nodes-added-function (clojure.set/difference nodes @previous-nodes))
        (reset! previous-nodes nodes)))))


(defn
  incremental-node-printer
  [node-retrieval-function]
  (incremental-node-manager 
    node-retrieval-function
    (fn [added] (println "Found "  (count added) " new nodes."))
    (fn [removed] (println "Found "  (count removed) " removed nodes."))))


(defn
  marker-utility
  []
  (MarkerUtility/getInstance))


(defn
  incremental-marker-updater
  [node-retrieval-function]
  (let [node-to-marker (atom {})]
    (incremental-node-manager 
      node-retrieval-function
      (fn [added]
        (swap!
         node-to-marker
         merge
         (zipmap
           added
           (map (fn [n]
                  (.tempNewMarker ^MarkerUtility (marker-utility) n))
                added))))
      (fn [removed]
        (doseq [n removed]
          (let [marker (get @node-to-marker n)]
            (when
              (.exists marker)
              (.delete marker))))))))

(defn-
  incremental-node-marker
  [node-retrieval-function]
  (let [update-markers
        (incremental-marker-updater node-retrieval-function)]
    (fn [model]
      (update-markers model))))

(defn
  nodes
  []
  (map first (damp.ekeko/ekeko [?m] 
                    (fresh [?t]
                           (relations/method-always-returning-null-for-ctype ?m ?t)))))
(def
  printer-listener
  (model-update-listener (incremental-node-printer nodes)))

(def 
  marker-listener
  (model-update-listener (incremental-node-marker nodes)))
            

;(ekekomodel/register-listener printer-listener)
                             
;(ekekomodel/register-listener marker-listener)

;(ekekomodel/unregister-listener marker-listener)

(comment
  (use 'damp.ekeko.demo.markers)
  (in-ns 'damp.ekeko.demo.markers)
  
  (ekekomodel/register-listener 
    (model-update-listener 
      (incremental-node-marker
        (fn []
          (map first 
            (damp.ekeko/ekeko [?m] 
              (fresh [?t]
                 (relations/method-always-returning-null-for-ctype ?m ?t))))))))
  
  )




