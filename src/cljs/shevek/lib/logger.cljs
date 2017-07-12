(ns shevek.lib.logger)

(def debug? ^boolean goog.DEBUG)

(defn log [& args]
  (if debug?
    (apply js/console.log args)))
