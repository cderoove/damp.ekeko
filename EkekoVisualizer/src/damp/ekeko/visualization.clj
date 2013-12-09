(ns damp.ekeko.visualization
  (:require 
    [damp.ekeko]
    [damp.ekeko.visualization [view :as graph]]))


(defn
  open-graph-view
  [node1tuples edge2tuples & {:as options}] 
  (let [nodes 
        (map first node1tuples)
        edgemap 
        (group-by first edge2tuples)
        context
        (apply 
          graph/make-graphicalcontext 
          (apply concat ;converts into sec of pairs
                 (assoc
                   options
                   :connections
                   (fn [element] 
                     (into-array (map second (get edgemap element)))))))
        view
        (graph/open-graph-view)]
    (graph/set-view-context! view context)
    (graph/set-view-elements! view nodes)))

(comment

  
  (require 'damp.ekeko.visualization)
  
  (damp.ekeko.visualization/open-graph-view 
    ;nodes
    (damp.ekeko/ekeko [?m] (damp.ekeko.jdt.reification/ast :MethodDeclaration ?m))
    ;edges
    (damp.ekeko/ekeko [?caller ?callee]
           (clojure.core.logic/fresh [?inv] 
                  (damp.ekeko.jdt.reification/ast :MethodDeclaration ?caller)
                  (damp.ekeko.jdt.reification/child+ ?caller ?inv)
                  (damp.ekeko.jdt.reification/ast :MethodInvocation ?inv)
                  (damp.ekeko.jdt.basic/methodinvocation-methoddeclaration ?inv ?callee)))
    :node|label
    (fn [method] (str (.getName method)))
    :edge|style 
    (fn [src dest] damp.ekeko.visualization.view/edge|directed)
    :layout
    damp.ekeko.visualization.view/layout|tree
    )
    
  
  )

    
            
    
        