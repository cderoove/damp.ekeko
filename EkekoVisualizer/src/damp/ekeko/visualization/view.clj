(ns damp.ekeko.visualization.view
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

;; Opening a view
;; --------------

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


(def layout|tree (ui (new TreeLayoutAlgorithm  LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|horizontaltree (ui (new HorizontalTreeLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|radial (ui (new RadialLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|grid (ui (new GridLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|spring (ui (new SpringLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def layout|shift (ui (new HorizontalShift LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
;compisite layout ontbreekt, nodig om bv tree-layout te combineren met horizontal-shift-layout zodat de nodes niet overlappen

(defn-
  shift
  [layout]
  (ui 
    (CompositeLayoutAlgorithm.  LayoutStyles/NO_LAYOUT_NODE_RESIZING
                              (into-array LayoutAlgorithm [layout|shift layout]))))
  

       

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
  
  (graph-set-layout! g layout|tree) 
  (graph-set-layout! g layout|spring)                 
  
  )


(def edge|dash (ZestStyles/CONNECTIONS_DASH))
(def edge|dashdot (ZestStyles/CONNECTIONS_DASH_DOT))
(def edge|directed (ZestStyles/CONNECTIONS_DIRECTED))
(def edge|dot (ZestStyles/CONNECTIONS_DOT))
(def edge|solid (ZestStyles/CONNECTIONS_SOLID))



;; Opening a model-based view
;; --------------------------

(def mgraph-view-cnt (atom 200))

(defn-
  show-mgraph-view
  []
  (let [page (gui/workbench-activepage)
        viewid (damp.ekeko.gui.views.EkekoVisualizationView/ID)
        uniqueid (str @mgraph-view-cnt)
        viewpart (.showView page viewid uniqueid (org.eclipse.ui.IWorkbenchPage/VIEW_ACTIVATE))]
    (swap! mgraph-view-cnt inc)
    (.setViewID viewpart uniqueid)
    viewpart))

(defn
  open-mgraph-view
  []
  (gui/eclipse-uithread-return (fn [] (show-mgraph-view))))


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
    (shift layout)
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

;; Query-based views (probably broken)
;; ===================================

(declare open-view-with-context)

;=> (group-by first [ [1 :a] [1 :b] [1 :c]  [2 :d] [2 :e]    ])
;{1 [[1 :a] [1 :b] [1 :c]], 2 [[2 :d] [2 :e]]}

(defn
  qview
  [node1tuples edge2tuples]
  (let [
        edgemap
        (group-by first edge2tuples)
        
        context 
        (ModelBasedView.     
          (proxy 
              [ArrayContentProvider IGraphEntityContentProvider]
              []
              (getConnectedTo [element] 
                (into-array (map second (get edgemap element)))))
          (proxy 
            [LabelProvider] 
            
            ;implementatie best als een dictionary met symbol als key, function als value
              ;de dict van de gebruiker mergen met een dict van defaults (waarvan de meeste functies null teruggeven)
              
              ;extra interfaces:  
              ;IEntityStyleProvider getBackground, borderWidth, .... om fill color, border width van nodes te veranderen
              ;IEntityConnectionStyleProvider connection style van edges 
              
              
              ;of 1 alternatieve interface:
              ;alle nodes en edges stylen ahv interface ISelfStyleProvider, waarin setBackGroundColor etc opgeroepen kan worden op elke edge/node
              
              
              []
              (getText [object]
                (if
                  (instance? EntityConnectionData object)
                  ""
                  ; (str (.source object) "->" (.dest object)) 
                  (str (.getClass object))))
              ;te overriden van LabelProvider: getImage om figuren in de nodes te tonen
              ;daarna extra interface methods implementeren
              )
          spring-layout 
          nil
          
          )
        
        nodes
        (map first node1tuples)
        ]
    
    (open-view-with-context nodes context)
    
    ))
          


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
  (let [view (open-mgraph-view)]
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
     (open-mgraph-view)
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
    