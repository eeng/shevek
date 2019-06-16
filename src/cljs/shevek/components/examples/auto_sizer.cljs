(ns shevek.components.examples.auto-sizer
  (:require [shevek.components.auto-sizer :refer [auto-sizer]]))

(defn example-page []
  [:div#outer {:style {:height "90vh" :width "90%" :background "orange"}}
   [auto-sizer
    (fn [{:keys [width height]}]
      [:div#inner {:style {:display "flex" :justify-content "center" :padding-top "1em" :font-size "300%"}}
       (str width " x " height)])]])
