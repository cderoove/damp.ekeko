(ns 
  ^{:doc "Utility functions for interacting with the Eclipse GUI and opening a Barista view on core.logic results."
    :author "Coen De Roover"}
   damp.ekeko.gui
   (:import [org.eclipse.ui IWorkbench PlatformUI IWorkbenchPage IWorkingSet IWorkingSetManager]
            [org.eclipse.swt.widgets Display]
            [org.eclipse.jface.window Window]
            [baristaui.views.queryResult QueryView SOULLabelProvider]))

;; UI thread helpers
;; -----------------
(defn 
  eclipse-uithread-do
  "Calls its Runnable argument synchronously in the Eclipse UI thread."
  [runnable]
  (.syncExec (Display/getDefault) runnable))

(defn 
  eclipse-uithread-return
  "Returns the value of its Runnable argument, called synchronously in the Eclipse UI thread."
  [runnable]
  (let [result (atom nil)]
    (eclipse-uithread-do (fn [] (reset! result (runnable))))
    @result))


;; Workbench
;; ---------

(defn 
  ^IWorkbench 
  get-workbench
  "Returns the Eclipse workbench."
  []
  (PlatformUI/getWorkbench))
  
(defn 
  get-active-workbench-window
  "Returns the active workbench window."
  []
  (.getActiveWorkbenchWindow (get-workbench))) 

(defn 
  workbench-window-shell 
  "Returns the shell of the given window."
  [^Window window]
  (.getShell window))

(defn
  workbench-activepage 
  "Returns the active workbench page. Must be called from UI thread."
  []
  (-> (PlatformUI/getWorkbench)
    .getActiveWorkbenchWindow ;nil if called from non-ui thread 
    .getActivePage))

(defn
  workbench-activepart
  "Returns the editor or view part that is currently active in the workbench. Must be called from UI thread."
  []
  (.getActivePart (workbench-activepage)))


(defn
  workbench-actionbars
  "Returns the IActionBars of the workbench. Must be called from UI thread."
  []
  (-> 
    (workbench-activepart)
    (.getSite)
    (.getActionBars)))

(defn 
  workbench-statuslinemanager
  "Returns the StatusLineManager of the workbench. Must be called from UI thread."
  []
  (.getStatusLineManager (workbench-actionbars)))

(defn
  set-workbench-status|error!
  "Displays an error message in the status line of the workbench.  Must be called from UI thread."
  [txt]
  (.setErrorMessage 
    (workbench-statuslinemanager)
    txt)) 


;; Selecting an IWorkingSet
;; ------------------------

(defn 
  select-workingset*
  "Opens a WorkingSetSelectionDialog that returns the _single_, non-aggregate selected WorkingSet or nil."
  []
  (let [manager (.getWorkingSetManager (get-workbench))
        shell (workbench-window-shell (get-active-workbench-window))
        dialog (.createWorkingSetSelectionDialog manager shell true)
        result (.open dialog)]
        (when (= result (Window/OK))
          (let [selection (.getSelection dialog)]
            (when (= 1 (alength selection))
              (aget selection 0))))))
        

;; View for query results
;; ----------------------
(def viewer-cnt (atom 0))


(def label-provider-class SOULLabelProvider)

(defn 
  open-barista-results-viewer* 
  "Opens and returns a Barista view on core.logic results. 
   Creates an instance of damp.ekeko.gui/label-provider-class as the label provider for the results."
  [querytxt logicvars resultsqc elapsed cnt]
  (let [tomap 
        (fn [sqc]
          (let [wrap-nil (fn [result] (if (nil? result) "nil" result)) 
                vars (map str logicvars)
                hmp (new java.util.HashMap (count sqc))]
            (doseq [v vars]
              (.put hmp v (new java.util.LinkedList)))
            (doseq [resultv sqc
                    [idx rslt] (map vector (iterate clojure.core/inc 0) resultv)]
              (.add ^java.util.LinkedList (.get hmp (nth vars idx)) (wrap-nil rslt)))
            hmp))
        resultmap (tomap resultsqc)
        page (-> (PlatformUI/getWorkbench)
                 .getActiveWorkbenchWindow ;nil if called from non-ui thread 
                 .getActivePage)
        qvid "ekeko.BaristaUI.queryResults"
        uniqueid (str @viewer-cnt)
        ^baristaui.views.queryResult.QueryView viewpart (.showView page qvid uniqueid (IWorkbenchPage/VIEW_ACTIVATE))]
    (swap! viewer-cnt inc)
    (.setViewID viewpart uniqueid)
    (.setQuery viewpart querytxt)
    (.setLabelProvider viewpart (clojure.lang.Reflector/invokeConstructor 
                                  label-provider-class
                                  (to-array [])))
    (.updateResultViews viewpart resultmap elapsed cnt)
    viewpart))

  











