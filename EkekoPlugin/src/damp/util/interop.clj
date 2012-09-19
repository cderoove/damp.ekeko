(ns damp.util.interop
   ^{:doc "Java interoperability utilities."
    :author "Coen De Roover"}
  (:import 
    [java.lang Class]
    [java.lang.reflect Field]
    ))

;from former (ns clojure.contrib.reflect)
(defn get-invisible-field
  "Copied from former (ns clojure.contrib.reflect) because
   I couldn't figure out where it resides these days.

   Access to private or protected field.  field-name is a symbol or
   keyword."
  [klass field-name obj]
  (-> ^Class klass ^Field (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

;from former (ns clojure.contrib.reflect)
(defn call-invisible-method
  "Copied from former (ns clojure.contrib.reflect) because
   I couldn't figure out where it resides these days.

   Calls a private or protected method.

   params is a vector of classes which correspond to the arguments to
   the method e

   obj is nil for static methods, the instance object otherwise.

   The method-name is given a symbol or a keyword (something Named)."
  [klass method-name params obj & args]
  (-> klass (.getDeclaredMethod (name method-name)
                                (into-array Class params))
      (doto (.setAccessible true))
      (.invoke obj (into-array Object args))))

