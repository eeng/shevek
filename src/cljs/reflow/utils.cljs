(ns reflow.utils)

(defn log [& args]
  (apply js/console.log args))
