(ns damp.ekeko.util.text
  (:require [clojure.pprint])
  (:require [clojure.string :as str :only [take split-lines]]))

(defn
  pprint-query-str
  "Returns the pretty printed string corresponding to the given Ekeko query."
  [query]
  (binding [clojure.pprint/*print-right-margin* 200]
    (with-out-str (clojure.pprint/pprint query))))


(defn 
  ellipsis
  "Ensures the string txt is no longer than 150 (or the given len)  
   of characters. Adds ... at the end."
  ([txt] (ellipsis txt 150))
  ([txt len] (str (take (- len 3) (first (str/split-lines txt))) "...")))