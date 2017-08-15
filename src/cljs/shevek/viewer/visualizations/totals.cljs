(ns shevek.viewer.visualizations.totals
  (:require [shevek.viewer.shared :refer [format-measure]]))

(defn- sort-results-according-to-selected-measures [{:keys [results measures]}]
  (let [result (first results)]
    (map #(assoc % :value (format-measure % result)) measures)))

(defn totals-visualization [viz]
  (let [result (sort-results-according-to-selected-measures viz)]
    [:div.ui.statistics
     (for [{:keys [name title value]} result]
       [:div.statistic {:key name}
        [:div.label title]
        [:div.value value]])]))
