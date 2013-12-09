(ns damp.ekeko.visualization
  (:refer-clojure :exclude [== type])
  (:use clojure.core.logic)
  (:use [damp.ekeko logic])
  (:use [damp.ekeko.soot soot])
  (:use [damp.ekeko.jdt ast structure aststructure soot convenience])
  (:use [damp.qwal])
  (:use [damp.ekeko])
  (:use [damp.ekeko.visualization.view]))


(defn
  open-visualization-view
  [node1tuples edge2tuples & {:as options}] 
  (let [nodes 
        (map first node1tuples)
        edgemap 
        (group-by first edge2tuples)
        context
        (apply 
          make-graphicalcontext 
          (apply concat ;converts into sec of pairs
                 (assoc
                   options
                   :connections
                   (fn [element] 
                     (into-array (map second (get edgemap element)))))))
        view
        (open-graph-view)]
    (set-view-context! view context)
    (set-view-elements! view nodes)))

(comment

  
  (require 'damp.ekeko.visualization)
  (in-ns 'damp.ekeko.visualization)

  
  (open-visualization-view
    ;nodes
    (ekeko [?m] (ast :MethodDeclaration ?m))
    ;edges
    (ekeko [?caller ?callee]
           (fresh [?inv] 
                  (ast :MethodDeclaration ?caller)
                  (child+ ?caller ?inv)
                  (ast :MethodInvocation ?inv)
                  (methodinvocation-methoddeclaration ?inv ?callee)))
    :node|label
    (fn [method] (str (.getName method)))
    :edge|style 
    (fn [src dest] edge|directed)
    :layout
    layout|tree)
    
  
  )

    
            
    
        