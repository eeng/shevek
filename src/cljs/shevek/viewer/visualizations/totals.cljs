(ns shevek.viewer.visualizations.totals
  (:require [shevek.viewer.shared :refer [format-measure]]))

(defn- sort-results-according-to-selected-measures [viewer]
  (let [result (first (get-in viewer [:results :main]))]
    (map #(assoc % :value (format-measure % result))
         (viewer :measures))))

(defn totals-visualization [viewer]
  (let [result (sort-results-according-to-selected-measures viewer)]
    [:div.ui.statistics
     (for [{:keys [name title value]} result]
       [:div.statistic {:key name}
        [:div.label title]
        [:div.value value]])]))
