(ns 
  ^{:doc "Inspect the results of Ekeko queries with an Inspector Jay view.
          https://github.com/timmolderez/inspector-jay"
    :author "Tim Molderez"}
  damp.ekeko.inspector.inspector
  (:refer-clojure :exclude [== type])
  (:use clojure.core.logic)
  (:use [damp.ekeko logic])
  (:use [damp.ekeko.soot soot])
  (:use [damp.ekeko.jdt ast structure aststructure soot convenience])
  (:use [damp.qwal])
  (:use [inspector-jay.core])
  (:use [inspector-jay.gui.gui])
  (:require [damp.ekeko.util [text :as text]])
  (:require [damp.ekeko [gui :as gui]]))

; Give each view instance a unique id
(def inspector-view-cnt (atom 0))

(defn get-inspector-view [results]
  "Build a ViewPart with an Inspector Jay panel that inspects 'results'"
  (let [page (gui/workbench-activepage)
        viewid (damp.ekeko.inspector.JPanelView/ID)
        uniqueid (str @inspector-view-cnt)
        viewpart (.showView page viewid uniqueid (org.eclipse.ui.IWorkbenchPage/VIEW_ACTIVATE))]
    (swap! inspector-view-cnt inc)
    (-> viewpart (.setJPanel (inspector-panel results)))
    viewpart))

(defn
  show-inspector-view
  "Opens Inspector Jay on given argument."
  [object]
  (gui/eclipse-uithread-return (fn [] (get-inspector-view object))))

(defmacro 
  ekeko+
  "Runs an Ekeko query and opens an Inspector Jay view to inspect the results
   See also: damp.ekeko/ekeko"
  [vars & goals]
  `(let [resultsqc# (doall (run-nc* [resultvar#] 
                             (fresh [~@vars]
                               (equals resultvar# [~@vars])
                               ~@goals)))]
     (show-inspector-view resultsqc#)))

(defn
  register-callbacks
  []
  (set! (baristaui.actions.InspectAction/FN_INSPECT_USING_JAY) show-inspector-view))

(register-callbacks)