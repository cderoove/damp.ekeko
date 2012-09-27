(ns damp.ekeko.soot.projectmodel
  (:import 
    [damp.ekeko.soot SootProjectModel])
  (:require
    [damp.ekeko [ekekomodel :as ekekomodel]]
    [damp.ekeko.workspace [workspace :as workspace]]
    ))




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


(def sootnature (damp.ekeko.soot.SootNature/NATURE_ID))

(defn
  enable-soot-nature!
  "Enables Ekeko's Soot nature on the given project. 
   Side effects: will start the whole-program analysis, 
   nature will be disabled for all other projects."
  [project]
  (workspace/enable-project-nature! project sootnature)
  (workspace/build-project project))

(defn
  disable-soot-nature!
  "Disables Ekeko's Soot nature for the given project."
  [project]
  (workspace/disable-project-nature! project sootnature))

(defn
  workspace-disable-soot!
  "Disables Ekeko'Soot nature on all workspace projects."
  []
  (damp.util.Natures/removeNatureFromAllProjects sootnature))
  
