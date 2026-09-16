(ns
  ^{:doc "Wires damp.ekeko.jdt.astnode's project-value wrap/unwrap functions
          into Barista's SWT results table label provider. Split out of
          damp.ekeko.jdt.astnode (which every relation namespace ultimately
          requires) so that loading the query engine doesn't require
          baristaui -- and therefore an Eclipse workbench -- to be present.
          Required from damp.ekeko.gui, which is itself only loaded when
          the Barista results view is actually opened (see ekeko*/ekeko-n*
          in damp.ekeko), preserving the original plugin's behavior of
          registering these callbacks whenever Barista's UI is used."
    :author "Coen De Roover"}
  damp.ekeko.jdt.baristacallbacks
  (:require [damp.ekeko.jdt [astnode :as astnode]])
  (:import [baristaui.views.queryResult SOULLabelProvider]))

(defn
  register-callbacks
  []
  (set! (SOULLabelProvider/FN_ISWRAPPER) astnode/value?)
  (set! (SOULLabelProvider/FN_GETWRAPPEDVALUE) astnode/value-unwrapped))

(register-callbacks)
