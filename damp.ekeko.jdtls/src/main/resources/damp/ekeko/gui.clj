(ns
  ^{:doc "This bundle's own stand-in for the Eclipse plugin's SWT-based
          damp.ekeko.gui (damp.ekeko.plugin/src/damp/ekeko/gui.clj, excluded
          from this bundle -- see pom.xml). damp.ekeko requires whichever
          damp.ekeko.gui its host supplies; this one notifies the VS Code
          extension instead of opening Barista SWT views."}
  damp.ekeko.gui
  (:require [damp.ekeko.middleware :as middleware])
  (:import (damp.ekeko.jdtls EkekoResultsBridge)))

;; Cancels each running query (as a no-argument function), by its id.
(defonce ^:private cancellers (atom {}))

(defn with-query-progress
  "Calls `f`, which runs the query `querystr`, and returns its value. Tells
   the VS Code extension when the query starts and when it ends (normally,
   by an exception or by being interrupted), so it can show it is running
   and warn when it was stopped by the REPL's eval timeout. See
   EkekoResultsBridge.queryStarted/queryFinished. While it runs, the query
   can be cancelled by its id, see cancel-query."
  [querystr f]
  (let [id (str (java.util.UUID/randomUUID))]
    (when-let [cancel (middleware/cancel-current-eval)]
      (swap! cancellers assoc id cancel))
    (EkekoResultsBridge/queryStarted id querystr)
    (try
      (f)
      (finally
        (swap! cancellers dissoc id)
        (EkekoResultsBridge/queryFinished id (middleware/expired-timeout))))))

(defn cancel-query
  "Interrupts the nREPL evaluation running the query with the id given to
   it by with-query-progress, in the background. Returns whether that query
   was still running and has been cancelled."
  [id]
  (if-let [cancel (get @cancellers id)]
    (do (future (cancel)) true)
    false))

(defn eclipse-uithread-return
  "The Eclipse plugin's counterpart runs its argument on the mandatory SWT
   UI thread. jdt.ls has no UI thread of its own to marshal onto, so just
   call it directly."
  [f]
  (f))

(defn open-barista-results-viewer*
  "The Eclipse plugin's counterpart opens a Barista SWT view on the
   results. Pushes the same data to the VS Code extension host instead,
   which opens/updates the results webview there -- see
   EkekoResultsBridge.showResults."
  [querystr vars results elapsed cnt]
  (EkekoResultsBridge/showResults querystr vars results elapsed cnt))
