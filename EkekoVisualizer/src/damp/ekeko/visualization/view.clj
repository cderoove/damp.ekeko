(ns damp.ekeko.visualization.view
  (:require [damp.ekeko [gui :as gui]])
  (:import [org.eclipse.zest.core.widgets GraphNode GraphConnection]
           [org.eclipse.zest.layouts.algorithms  CompositeLayoutAlgorithm TreeLayoutAlgorithm HorizontalTreeLayoutAlgorithm RadialLayoutAlgorithm GridLayoutAlgorithm SpringLayoutAlgorithm HorizontalShift]  
           [org.eclipse.swt SWT]
           [org.eclipse.zest.layouts LayoutStyles LayoutAlgorithm]
           [org.eclipse.zest.core.viewers  IGraphEntityContentProvider EntityConnectionData IEntityStyleProvider]
           [org.eclipse.jface.viewers ArrayContentProvider LabelProvider]
           [org.eclipse.ui.plugin AbstractUIPlugin]
           [org.eclipse.swt.widgets Display]
           [org.eclipse.swt SWT]
           [org.eclipse.jface.resource JFaceResources LocalResourceManager]
           [org.eclipse.swt.graphics RGB]

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


(def tree-layout (ui (new TreeLayoutAlgorithm  LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def horizontal-tree-layout (ui (new HorizontalTreeLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def radial-layout (ui (new RadialLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def grid-layout (ui (new GridLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def spring-layout (ui (new SpringLayoutAlgorithm LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
(def horizontal-shift-layout (ui (new HorizontalShift LayoutStyles/NO_LAYOUT_NODE_RESIZING)))
;compisite layout ontbreekt, nodig om bv tree-layout te combineren met horizontal-shift-layout zodat de nodes niet overlappen

(defn-
  horizontal-shift
  [layout]
  (ui 
    (CompositeLayoutAlgorithm.  LayoutStyles/NO_LAYOUT_NODE_RESIZING
                              (into-array LayoutAlgorithm [horizontal-shift-layout layout]))))
  

       

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
    (.getViewer viewpart)))

(defn
  open-mgraph-view
  []
  (gui/eclipse-uithread-return (fn [] (show-mgraph-view))))


(defrecord ModelBasedView [content label layout])

(defn
  make-graphicalcontext
  [& {
      :keys
      [layout
       connections 
       node|label 
       edge|label
       node|image
       edge|image
       node|zoomed
       node|border|color
       edge|border|color
       node|border|color|highlight 
       edge|border|color|highlight 
       node|border|width
       edge|border|width
       node|color|foreground
       edge|color|foreground
       node|color|background
       node|color|highlight
       node|tooltip
       ] 
      :or 
      {connections (fn [element] [])
       node|label (fn [node] "")        
       edge|label (fn [edge] "")
       node|image (fn [node] nil)
       edge|image (fn [edge] nil)
       node|zoomed (fn [node] false)
       node|border|color (fn [node] nil)
       edge|border|color (fn [edge] nil)
       node|border|width (fn [node] 1)
       node|border|color|highlight (fn [node] nil)
       edge|border|color|highlight (fn [edge] nil)
       node|color|foreground (fn [node] nil)
       edge|color|foreground (fn [edge] nil)
       node|color|background (fn [node] nil)
       node|color|highlight (fn [node] nil)
       node|tooltip (fn [node] nil)
       layout tree-layout }
      }]
  (ModelBasedView.
    (proxy 
      [ArrayContentProvider IGraphEntityContentProvider]
      []
      (getConnectedTo [element]   
        (connections element))) 
    (proxy 
        [LabelProvider IEntityStyleProvider]
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
        (fisheyeNode [object]
          (node|zoomed object))
        (getBorderWidth [object]
          (node|border|width object))
        (getBorderColor [object]
          (if
            (instance? EntityConnectionData object)
            (edge|border|color object)
            (node|border|color object)))
        (getBorderHighlightColor [object]
          (if
            (instance? EntityConnectionData object)
            (edge|border|color|highlight object)
            (node|border|color|highlight object)))
        (getNodeHighlightColor [node]
          (node|color|highlight node))
        (getBackgroundColour [node]
          (node|color|background node))
        (getForegroundColour [object]
          (if
            (instance? EntityConnectionData object)
            (edge|color|foreground object)
            (node|color|foreground object)))
        (getTooltip [node]
          (node|tooltip node)))
    
    (horizontal-shift layout)))



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
          
          )
        
        nodes
        (map first node1tuples)
        ]
    
    (open-view-with-context nodes context)
    
    ))
          


(defn
  set-view-elements!
  [view elements]    
  (ui 
    (.setInput view elements)
    (graph-layout! (.getControl view))))

(defn
  set-view-context!
  [view context]
  (ui
    (.setContentProvider view (:content context))
    (.setLabelProvider view (:label context))
    (.setLayoutAlgorithm view (:layout context) true)
   ))
  
(defn
  open-view-on-elements-with-context
  [elements context]
  (let [v (open-mgraph-view)]
    (ui 
      (set-view-context! v context)
      (set-view-elements! v elements))))

(defn
  system-color
  [swt-color-constant]
  (.getSystemColor (Display/getCurrent) swt-color-constant))

(defn 
  color
  [view red green blue]
  (let [mngr 
        (LocalResourceManager. (JFaceResources/getResources) (.getControl view))]
    ;to be reused, map on clojure side ... or using binding http://clojuredocs.org/clojure_core/clojure.core/binding
    (.createColor mngr (RGB. red green blue))
  ))


  
(comment
  
  (let 
    [img 
     (.createImage (AbstractUIPlugin/imageDescriptorFromPlugin  damp.ekeko.Activator/PLUGIN_ID "/icons/ekeko46.png"))
     elements
     (range 10)
     view
     (open-mgraph-view)
     context 
     (make-graphicalcontext 
       :connections
       (fn [element] (into-array [(first x)]))
       :node|label
       (fn [node]  (str node))
       :edge|label
       (fn [edge] (str (.source edge) "->" (.dest edge)))
       :node|image
       (fn [node] img)
       :node|border|width 
       (fn [node] node)
       :node|border|color 
       (fn [node] (color view 100 10 14)))
     ]
    (set-view-context! view context)
    (set-view-elements! view elements)
    )
    
    
    
  
  
  
  
  
    )
    