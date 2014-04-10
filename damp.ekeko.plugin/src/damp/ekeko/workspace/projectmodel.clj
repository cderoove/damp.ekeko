(ns 
 ^{:doc "Central point of access to ProjectModel instances managed by EkekoModel for reification relations."
   :author "Reinout Stevens"}
  damp.ekeko.workspace.projectmodel
  (:require [damp.ekeko [ekekomodel :as ekekomodel]]))

; Eclipse Projects
; ----------------

(defn get-members [project-model]
  (seq (.members (.getProject project-model))))

(defn get-deep-members 
  ([project-model]
    (get-deep-members project-model (fn [x] true)))
  ([project-model filterfn]
  (defn icontainer-members [container]
    (let [mem (filter filterfn (seq (.members container)))
          containers (filter (fn [x]
                               (instance? org.eclipse.core.resources.IContainer x))
                             mem)
          mapcatted (mapcat icontainer-members containers)]
      (concat mem mapcatted)))
  (icontainer-members (.getProject project-model))))


(defn visible? [iresource]
  (not-any? (fn [x] (.startsWith x ".")) (seq (.segments (.getFullPath iresource)))))

(defn hidden? [iresource]
  (not (visible? iresource)))


(defn file-extensions [project-model]
  (seq
    (set
      (map
        (fn [iresource]
          (.getFileExtension (.getFullPath iresource))) 
        (filter 
          (fn [x]
            (not (instance? org.eclipse.core.resources.IFolder x)))
          (get-deep-members project-model visible?))))))


