(defproject Ekeko "1.0.3"
  :description "Applicative logic meta-programming using core.logic against an Eclipse workspace."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;:dev-dependencies [[lein-autodoc "0.9.0"]]
  ;:dev-dependencies [[codox "0.6.1"]]
  
    :codox {
            :name "Ekeko"
            :version "1.0.3"
            :description "Applicative logic meta-programming using core.logic against an Eclipse workspace."
            :output-dir "/Users/cderoove/Desktop/docekeko"
            :sources ["src"]
            :src-dir-uri "https://github.com/cderoove/damp.ekeko/blob/master/EkekoPlugin"
            :src-linenum-anchor-prefix "L"}

  
  
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/core.logic "0.7.5"]
                 [qwal "1.0.0-SNAPSHOT"]])
