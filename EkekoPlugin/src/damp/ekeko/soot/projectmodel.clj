(ns damp.ekeko.soot.projectmodel
  (:import 
    [damp.ekeko.soot SootProjectModel])
  (:require
    [damp.ekeko [ekekomodel :as ekekomodel]]))




(defn 
  soot-project-models
  "Returns all SootProjectModel instances that are to be queried.
   Subset of (queried-project-models), which is itself 
   a subset of (all-project-models)."
  []
  (filter (fn [project-model] (instance? SootProjectModel project-model))
          (ekekomodel/queried-project-models)))


(defn
  ^SootProjectModel
   current-soot-model
  "Currently, there should only be one SootProjectModel as Soot itself
    relies on global state internally."
  []
  (first (soot-project-models)))

