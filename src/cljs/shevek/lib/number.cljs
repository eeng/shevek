(ns shevek.lib.number
  (:require cljsjs.numeral))

(defn format [number format]
  (-> number js/numeral (.format format)))
