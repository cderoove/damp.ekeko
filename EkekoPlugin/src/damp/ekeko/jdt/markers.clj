(ns damp.ekeko.jdt.markers
    (:import [org.eclipse.ui.IMarkerResolution]))

(def markers (atom '()))

(defn add-problem-marker [astnode message kind]
  (let [compunit (.getRoot astnode)
        start-pos (.getStartPosition astnode)
        line-no (.getLineNumber compunit start-pos)
        resource (.getCorrespondingResource (.getJavaElement compunit))
        marker (.createMarker resource "damp.ekeko.plugin.ekekoproblemmarker")]
    (.setAttribute marker org.eclipse.core.resources.IMarker/LINE_NUMBER (int line-no))
    (.setAttribute marker org.eclipse.core.resources.IMarker/CHAR_START (int start-pos))
    (.setAttribute marker org.eclipse.core.resources.IMarker/CHAR_END (int (+ start-pos (.getLength astnode))))
    (.setAttribute marker 
      org.eclipse.core.resources.IMarker/SEVERITY
      (int org.eclipse.core.resources.IMarker/SEVERITY_WARNING))
    (.setAttribute marker 
      org.eclipse.core.resources.IMarker/MESSAGE 
      message)
    (.setAttribute marker "ekekoKind" kind)
    (.setAttribute marker "astnode" astnode) ;;pray this works
    (swap! markers conj marker)
    marker))

(defn reset-and-delete-markers []
  (do
    (map #(.delete %) @markers)
    (reset! markers '())))

(defn reset-and-delete-marker [a-marker]
  (do
    (.delete a-marker)
    (swap! markers (fn [markers] (filter #(.exists %1) markers)))))

(defn ekekomarker-astnode [marker]
  (.getAttribute marker "astnode"))


(defn create-quick-fix [label fix]
 (reify
    org.eclipse.ui.IMarkerResolution
    (getLabel [this] label)
    (run [this marker] 
      (fix marker))))
