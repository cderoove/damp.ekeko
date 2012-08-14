(ns damp.ekeko.util.text
  (:require [clojure.string :as str :only [take split-lines]]))

(defn 
  ellipsis
  "Ensures the string txt is no longer than 150 (or the given len)  
   of characters. Adds ... at the end."
  ([txt] (ellipsis txt 150))
  ([txt len] (str (take (- len 3) (first (str/split-lines txt))) "...")))