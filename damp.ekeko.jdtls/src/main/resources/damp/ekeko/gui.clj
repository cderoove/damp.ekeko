(ns
  ^{:doc "This bundle's own stand-in for the Eclipse plugin's SWT-based
          damp.ekeko.gui (damp.ekeko.plugin/src/damp/ekeko/gui.clj, excluded
          from this bundle -- see pom.xml). damp/ekeko.clj's ekeko*/ekeko-n*
          macros resolve damp.ekeko.gui/eclipse-uithread-return and
          damp.ekeko.gui/open-barista-results-viewer* via requiring-resolve
          rather than a static require specifically so a host with no
          Eclipse workbench (this one) can supply its own; unchanged
          otherwise, this is that plug-in point, pushing results to the VS
          Code results webview instead of opening a Barista SWT view."}
  damp.ekeko.gui)

(defn eclipse-uithread-return
  "The Eclipse plugin's counterpart runs its argument on the mandatory SWT
   UI thread. jdt.ls has no UI thread of its own to marshal onto, so just
   call it directly."
  [f]
  (f))

(defn open-barista-results-viewer*
  "The Eclipse plugin's counterpart opens a Barista SWT view on the
   results. Pushes the same data to the VS Code extension host instead (a
   fire-and-forget workspace/notify, per JavaClientConnection/
   ExecuteCommandProposedClient), which opens/updates the results webview
   there -- see EkekoResultsBridge.showResults and ekeko.showResults in
   ekeko-vscode/src/extension.ts."
  [querystr vars results elapsed cnt]
  (damp.ekeko.jdtls.EkekoResultsBridge/showResults querystr vars results elapsed cnt))
