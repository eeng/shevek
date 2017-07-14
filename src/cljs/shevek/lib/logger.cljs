(ns shevek.lib.logger)

(def debug? ^boolean goog.DEBUG)

(defn info [& args]
  (if debug?
    (apply js/console.log args)))

(defn error [& args]
  (apply js/console.error args))
