(ns shevek.viewer.visualizations.totals
  (:require [shevek.viewer.shared :refer [format-measure measure-value]]))

(defn totals-visualization [{:keys [results measures]}]
  (let [grand-total (first results)]
    [:div.ui.statistics
     (doall
       (for [{:keys [name title] :as measure} measures]
         [:div.statistic {:key name :title (measure-value measure grand-total)}
          [:div.label title]
          [:div.value (format-measure measure grand-total)]]))]))
