(defproject Ekeko "1.0.2"
  :description "Applicative logic meta-programming using core.logic against an Eclipse workspace."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :autodoc { :name "Ekeko", :page-title "Ekeko Documentation"}
  ;:dev-dependencies [[lein-autodoc "0.9.0"]]
  :dev-dependencies [[codox "0.6.1"]]

  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/core.logic "0.7.5"]
                 [qwal "1.0.0-SNAPSHOT"]])
