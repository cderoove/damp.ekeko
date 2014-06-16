(ns 
    ^{:doc "Eclipse ZEST-based visualizations."
  :author "Coen De Roover"}
  damp.ekeko.visualization.view
  (:require [damp.ekeko [gui :as gui]])
  (:import [org.eclipse.zest.core.widgets GraphNode GraphConnection]
           [org.eclipse.zest.layouts.algorithms  CompositeLayoutAlgorithm TreeLayoutAlgorithm HorizontalTreeLayoutAlgorithm RadialLayoutAlgorithm GridLayoutAlgorithm SpringLayoutAlgorithm HorizontalShift]  
           [org.eclipse.swt SWT]
           [org.eclipse.zest.layouts LayoutStyles LayoutAlgorithm]
           [org.eclipse.zest.core.viewers  IGraphEntityContentProvider EntityConnectionData IEntityStyleProvider IEntityConnectionStyleProvider]
           [org.eclipse.zest.core.widgets ZestStyles]
           [org.eclipse.jface.viewers ISelectionChangedListener ArrayContentProvider LabelProvider]
           [org.eclipse.ui.plugin AbstractUIPlugin]
           [org.eclipse.swt.widgets Display]
           [org.eclipse.swt SWT]
           [org.eclipse.swt.events SelectionAdapter SelectionEvent]
           [org.eclipse.jface.resource JFaceResources LocalResourceManager]
           [org.eclipse.swt.graphics RGB]
           [org.eclipse.draw2d Figure Ellipse FlowLayout Label MarginBorder StackLayout]
           ))


(defmacro ui [& exps] `(gui/eclipse-uithread-return (fn [] ~@exps)))

(defmacro
  defnui 
  [name argvector & exps]  
  `(defn ~name ~argvector(ui ~@exps)))
  
(defnui
  graph-nodes
  [graph]
  (.getNodes graph))


(def layout|tree (ui (new TreeLayoutAlgorithm  LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|horizontaltree (ui (new HorizontalTreeLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|radial (ui (new RadialLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|grid (ui (new GridLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|spring (ui (new SpringLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|shift (ui (new HorizontalShift LayoutStyles/NO_LAYOUT_NODE_RESIZING)))

(defn-
  shift-layout
  [layout]
  (ui 
    (CompositeLayoutAlgorithm.  LayoutStyles/NO_LAYOUT_NODE_RESIZING
                              (into-array LayoutAlgorithm [layout|shift layout]))))
(defnui
  graph-layout!
  [graph]
  (.applyLayout graph))

(def edge|dash (ZestStyles/CONNECTIONS_DASH))
(def edge|dashdot (ZestStyles/CONNECTIONS_DASH_DOT))
(def edge|directed (ZestStyles/CONNECTIONS_DIRECTED))
(def edge|dot (ZestStyles/CONNECTIONS_DOT))
(def edge|solid (ZestStyles/CONNECTIONS_SOLID))

;; Opening a model-based graph view
;; --------------------------------

(def graph-view-cnt (atom 0))

(defn-
  show-graph-view
  []
  (let [page (gui/workbench-activepage)
        viewid (damp.ekeko.gui.views.EkekoVisualizationView/ID)
        uniqueid (str @graph-view-cnt)
        viewpart (.showView page viewid uniqueid (org.eclipse.ui.IWorkbenchPage/VIEW_ACTIVATE))]
    (swap! graph-view-cnt inc)
    (.setViewID viewpart uniqueid)
    viewpart))

(defn
  open-graph-view
  []
  (gui/eclipse-uithread-return (fn [] (show-graph-view))))


(defrecord ModelBasedView [content label layout listener])

(defn
  make-graphicalcontext
  [& {
      :keys
      [layout
       connections 
       node|zoomed
       node|label 
       node|image
       node|border|color
       node|border|color|highlight 
       node|border|width
       node|color|foreground
       node|color|background
       node|color|highlight
       node|tooltip
       
       edge|label
       edge|image
       edge|color
       edge|color|highlight
       edge|style
       edge|width
       edge|tooltip
       
       node|selected
       edge|selected
       void|selected
       ] 
      :or 
      {layout layout|radial
       connections (fn [element] [])
       node|label (fn [node] "")        
       edge|label (fn [edge] "")
       node|image (fn [node] nil)
       node|zoomed (fn [node] false)
       node|border|color (fn [node] nil)
       node|border|width (fn [node] -1)
       node|border|color|highlight (fn [node] nil)
       node|color|foreground (fn [node] nil)
       node|color|background (fn [node] nil)
       node|color|highlight (fn [node] nil)
       node|tooltip (fn [node] nil)
       
       edge|image (fn [edge] nil)
       edge|color (fn [src dest] nil)
       edge|color|highlight (fn [src dest] nil)
       edge|style (fn [src dest] edge|solid)
       edge|width (fn [src dest] -1)
       edge|tooltip (fn [edge] nil)

       node|selected (fn [node] )
       edge|selected (fn [edge] )
       void|selected (fn [])
       
       }
      }]
  (ModelBasedView.
    (proxy 
      [ArrayContentProvider IGraphEntityContentProvider]
      []
      (getConnectedTo [element]   
        (connections element))) 
    (proxy 
        [LabelProvider IEntityStyleProvider IEntityConnectionStyleProvider]
        []
        (getText [object]
          (if
            (instance? EntityConnectionData object)
            (edge|label object)
            (node|label object)))
        (getImage [object]
          (if
            (instance? EntityConnectionData object)
            (edge|image object)
            (node|image object)))
        (getTooltip [object]
          (if
            (instance? EntityConnectionData object)
            (edge|tooltip object)
            (node|tooltip object)))
        (fisheyeNode [object]
          (node|zoomed object))
        (getBorderWidth [object]
          (node|border|width object))
        (getBorderColor [object]
          (node|border|color object))
        (getBorderHighlightColor [node]
          (node|border|color|highlight node))
        (getNodeHighlightColor [node]
          (node|color|highlight node))
        (getBackgroundColour [node]
          (node|color|background node))
        (getForegroundColour [node]
          (node|color|foreground node))
        (getColor [src dest]
          (edge|color src dest))
        (getConnectionStyle [src dest]
          (edge|style src dest))
        (getHighlightColor [src dest]
          (edge|color|highlight src dest))
        (getLineWidth [src dest]
          (edge|width src dest))
        )
   ;(shift-layout layout)
   layout 
   (proxy 
     [ISelectionChangedListener]
     []
     (selectionChanged [event]
       (let [selection (.getSelection event)
             selected (.getFirstElement selection)]
         (cond
           (nil? selected) (void|selected)
           (instance? EntityConnectionData selected) (edge|selected selected)
           :else (node|selected selected)))))))

(defn
  set-view-elements!
  [view elements]
  (let [viewer (.getViewer view)
        graph (.getControl viewer)]
    (ui 
      (.setInput viewer elements)
      (graph-layout! graph))))

(defn
  set-view-context!
  [view context]
  (let [viewer (.getViewer view)
        graph (.getControl viewer)]
    (ui
      (.setContentProvider viewer (:content context))
      (.setLabelProvider viewer (:label context))
      (.setLayoutAlgorithm viewer (:layout context) true)
      (.addSelectionChangedListener viewer (:listener context)))))
  
(defn
  open-view-on-elements-with-context
  [elements context]
  (let [view (open-graph-view)]
    (ui 
      (set-view-context! view context)
      (set-view-elements! view elements))))

(defn
  system-color
  [swt-color-constant]
  (.getSystemColor (Display/getCurrent) swt-color-constant))


(defn 
  color
  [view red green blue]
  (.createColor (.getResourceManager view) (RGB. red green blue)))

  
(comment
  
  (let 
    [node-img 
     (.createImage (AbstractUIPlugin/imageDescriptorFromPlugin  damp.ekeko.Activator/PLUGIN_ID "/icons/ekeko46.png"))
     edge-img
     (.createImage (AbstractUIPlugin/imageDescriptorFromPlugin  damp.ekeko.Activator/PLUGIN_ID "/icons/view-refresh.png"))
     elements
     (range 10)
     view
     (open-graph-view)
     context 
     (make-graphicalcontext 
       :connections
       (fn [element] 
         (if 
           (= element (first elements))
           (into-array elements)))
       :node|label
       (fn [node]  (str node))
       :edge|label
       (fn [edge] (str (.source edge) "->" (.dest edge)))
       :node|image
       (fn [node] node-img)
       :edge|image
       (fn [edge] edge-img)
       :node|border|width 
       (fn [node] node)
       :node|border|color 
       (fn [node] (color view 100 10 140))
       :node|border|color|highlight
       (fn [node] (color view 200 200 200))
       :node|color|foreground 
       (fn [node] (color view 0 0 0))
       :node|color|background 
       (fn [node] (color view 255 255 255))
       :node|color|highlight  
       (fn [node] (color view 230 200 10))
       :node|tooltip 
       (fn [node] 
         (let [fig (Figure.)
               layout (FlowLayout. false)
               ellipse (Ellipse.)]
           (.setSize ellipse 40 40)
           (.setBorder fig (MarginBorder. 5 5 5 5))
           (.setMajorSpacing layout 3)
           (.setMinorAlignment layout 3)
           (.setLayoutManager fig layout)
           (.add fig (Label. (str "Node: " node)))
           (.add fig ellipse) 
           fig))
       :edge|color 
       (fn [src dest] (color view 100 10 140))
       :edge|style 
       (fn [src dest] 
         (if 
           (= src dest)
           edge|dot
           (bit-or 
             edge|directed
             edge|solid)))
       :edge|color|highlight
       (fn [src dest] (color view 230 200 10))
       :edge|width
       (fn [src dest] 4)
       :node|selected
       (fn [node] (println "Selected node: " node))
       :edge|selected
       (fn [edge] (println "Selected edge: " edge))
       :void|selected
       (fn [] (println "Nothing selected anymore"))
       )
     ]
    
    (set-view-context! view context)
    (set-view-elements! view elements))
  
  
  
  )
    