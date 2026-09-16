(ns
  ^{:doc "Places Eclipse IMarkers (squiggly underlines) on nodes as they
          enter/leave a query's result set, backed by
          baristaui.util.MarkerUtility. Split out of damp.ekeko.ekekomodel
          (which the query engine's model-access layer requires) so that
          namespace can be loaded without baristaui -- and therefore an
          Eclipse workbench -- present. Require this namespace explicitly
          to use incremental-node-marker, exactly as before."
    :author "Coen De Roover"}
  damp.ekeko.baristamarkers
  (:require [damp.ekeko [ekekomodel :as ekekomodel]])
  (:import [baristaui.util MarkerUtility]))

(defn
  marker-utility
  []
  (MarkerUtility/getInstance))

(defn
  incremental-marker-updater
  [node-retrieval-function]
  (let [node-to-marker (atom {})]
    (ekekomodel/incremental-node-manager
      node-retrieval-function
      (fn [added]
        (swap!
         node-to-marker
         merge
         (zipmap
           added
           (map (fn [n]
                  (.tempNewMarker ^MarkerUtility (marker-utility) n))
                added))))
      (fn [removed]
        (doseq [n removed]
          (let [marker (get @node-to-marker n)]
            (when
              (.exists marker)
              (.delete marker))))))))

(defn
  incremental-node-marker
  [node-retrieval-function]
  (let [update-markers
        (incremental-marker-updater node-retrieval-function)]
    (fn [model]
      (update-markers model))))
