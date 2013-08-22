(ns damp.ekeko.visualization.view
  (:require [damp.ekeko [gui :as gui]])
  (:import [org.eclipse.zest.core.widgets GraphNode GraphConnection]
           [org.eclipse.zest.layouts.algorithms TreeLayoutAlgorithm HorizontalTreeLayoutAlgorithm RadialLayoutAlgorithm GridLayoutAlgorithm SpringLayoutAlgorithm HorizontalShift]  
           [org.eclipse.swt SWT]
           [org.eclipse.zest.layouts LayoutStyles]
          
           ))

;; Opening a view
;; --------------

(def graph-view-cnt (atom 0))

(defn-
  show-graph-view
  []
  (let [page (gui/workbench-activepage)
        viewid (damp.ekeko.visualization.EkekoVisualizationView/ID)
        uniqueid (str @graph-view-cnt)
        viewpart (.showView page viewid uniqueid (org.eclipse.ui.IWorkbenchPage/VIEW_ACTIVATE))]
    (swap! graph-view-cnt inc)
    (.setViewID viewpart uniqueid)
    (.getGraph viewpart)))

(defn
  open-graph-view
  []
  (gui/eclipse-uithread-return (fn [] (show-graph-view))))


;; Graph manipulation
;; ------------------

(defmacro ui [& exps] `(gui/eclipse-uithread-return (fn [] ~@exps)))

(defmacro
  defnui 
  [name argvector & exps]  
  `(defn ~name ~argvector(ui ~@exps)))

  
(defnui
  graph-nodes
  [graph]
  (.getNodes graph))

(defnui
  graph-edges
  [graph]
  (.getConnections graph))

(defnui 
  graph-add-node!
  [graph]
  ;create anonymous subclass of Zest's GraphNode to prevent toString from being called outside of the UI thread by the REPL
    (proxy
      [GraphNode] 
      [graph (.getNodeStyle graph)]
      (toString []
        ;needs to be called in UI thread
        (ui (proxy-super toString)))))

(defnui
  node-set-text!
  [node string]
  (.setText node string))

(defnui
  graph-add-edge!
  [graph from to]
  ;same as above
  (proxy 
    [GraphConnection]
    [graph (.getConnectionStyle graph) from to]
    (toString []
        ;needs to be called in UI thread
        (ui (proxy-super toString)))))

(defnui
  edge-set-text!
  [node string]
  (.setText node string))

(defnui
  graph-set-layout!
  [graph layout]
  ;note that the layout algorithm has to be created in the ui thread!
  (.setLayoutAlgorithm graph layout true))


(def tree-layout (ui (new TreeLayoutAlgorithm  LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def horizontal-tree-layout (ui (new HorizontalTreeLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def radial-layout (ui (new RadialLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def grid-layout (ui (new GridLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def grid-layout (ui (new GridLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def spring-layout (ui (new SpringLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def horizontal-shift-layout (ui (new HorizontalShift LayoutStyles/NO_LAYOUT_NODE_RESIZING)))




(defnui
  graph-layout!
  [graph]
  (.applyLayout graph))




(comment
  
  (def g (open-graph-view))
  
  (def root (graph-add-node! g))
  
  (dotimes [n 10] 
    (let [child (graph-add-node! g)]
      (node-set-text! child (str n))
      (graph-add-edge! g child root)))
  
  (graph-set-layout! g tree-layout) 
  (graph-set-layout! g spring-layout)                 
  
  )



  

    
  


