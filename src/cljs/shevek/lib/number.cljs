(ns shevek.lib.number
  (:require cljsjs.numeral
            [shevek.i18n :refer [lang]]))

(defonce locale-registration
  (.register js/numeral "locale" "es"
             (clj->js
              {:delimiters {:thousands "." :decimal ","}
               :abbreviations {:thousand "k" :million "m" :billion "b" :trillion "t"}
               :ordinal (fn [number] "°")
               :currency {:symbol "$"}})))

(defn format [number format]
  (.locale js/numeral (name (lang)))
  (-> number js/numeral (.format format)))
