(ns shevek.lib.logger
  (:require [cljs.pprint]))

(def debug? ^boolean goog.DEBUG)

(defn info [& args]
  (if debug?
    (apply js/console.log args)))

(defn error [& args]
  (apply js/console.error args))

(defn pp-str [& args]
  (with-out-str (apply cljs.pprint/pprint args)))
