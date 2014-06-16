(ns 
  ^{:doc "Quick visualization of Crystal's EclipseCFG graphs, used 
          as control flow graphs by Ekeko."
  :author "Coen De Roover"}

  damp.ekeko.visualization.cfgview
  (:refer-clojure :exclude [== type])
  (:require [clojure.core [logic :as cl]])
  (:require 
    [damp.ekeko.jdt 
     [ast :as ast]
     [convenience :as convenience]]
    [damp.ekeko.visualization 
     [view :as view]])
  (:import [edu.cmu.cs.crystal.cfg.eclipse EclipseCFG EclipseNodeFirstCFG]))
  

(defn
  node-edges|outgoing
  [cfgnode]
  (.getOutputs cfgnode))
  
      
(defn
  node-nodes|outgoing
  [cfgnode]
  (map (fn [edge] (.getSink edge))
       (node-edges|outgoing cfgnode)))
        

(defn
  cfg-nodes
  [cfg]
  (let [startNode (.getStartNode cfg)]
    (loop [handling 
           [startNode]
           nodes 
           #{}]
      (if 
        (empty? handling)
        nodes
        (let [node (first handling)
              outNodes (node-nodes|outgoing node)
              outNodesMinusVisited (remove (fn [n] (some #{n} nodes)) outNodes)]
          (recur (concat outNodesMinusVisited (rest handling))
                 (clojure.set/union nodes (set outNodes))))))))


(defn
  open-cfgview
  [cfg]
  (let [nodes 
        (cfg-nodes cfg)
        view
        (view/open-graph-view)
        context
        (view/make-graphicalcontext 
          :connections
          (fn [node] 
            (into-array (node-nodes|outgoing node)))
          :node|label
          (fn [node]  (str
                        node
                        "\n"
                        (.getASTNode node)))
          :edge|style 
          (fn [src dest] view/edge|directed)
          :layout
          view/layout|tree
          )]
    (view/set-view-context! view context)
    (view/set-view-elements! view nodes)
    view))
          

(comment 

  ;cfgs end with method declaration, preceded by one artificial uber-return node

  (for [[typeName methodName]
        [["Composite" "acceptVisitor"] ["MayAliasLeaf" "m"] ["TestCase" "runTest"]]]
    (let [method 
          (first 
            (first 
              (damp.ekeko/ekeko [?m] 
                                (cl/fresh [?t] 
                                          (convenience/typedeclaration-identifier-bodydeclaration-identifier
                                            ?t typeName
                                            ?m methodName)))))
          cfg
          ;(EclipseNodeFirstCFG. method)
          (ast/cfg-for-method method)
          ]
      (open-cfgview cfg)))
    
  )

  