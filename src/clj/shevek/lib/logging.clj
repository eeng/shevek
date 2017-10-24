(ns shevek.lib.logging
  (:require [taoensso.timbre :as log]))

(defn pp-str [& args]
  (with-out-str (apply clojure.pprint/pprint args)))

(defmacro benchmark [str & body]
  `(let [start# (System/nanoTime)]
     ~@body
     (log/debug (format ~str (/ (- (System/nanoTime) start#) 1e6)))))
