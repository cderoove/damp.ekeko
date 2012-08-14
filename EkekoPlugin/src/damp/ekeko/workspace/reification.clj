(ns 
  ^{:doc "Low-level ProjectModel reification relations."
    :author "Reinout Stevens"}
  damp.ekeko.workspace.reification
  (:refer-clojure :exclude [==])
  (:use [clojure.core.logic])
  (require 
    [damp.ekeko [ekekomodel :as ekekomodel]]
    [damp.ekeko.workspace [projectmodel :as projectmodel]]))

; Reification of Eclipse Resource Model
; -------------------------------------

(defn memberso [mems]
  (membero mems (mapcat projectmodel/get-members (ekekomodel/queried-project-models))))

(defn deep-memberso [mems]
  (membero mems (mapcat projectmodel/get-deep-members (ekekomodel/queried-project-models))))

   

