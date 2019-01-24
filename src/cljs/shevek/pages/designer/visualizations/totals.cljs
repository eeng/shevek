(ns shevek.pages.designer.visualizations.totals
  (:require [shevek.domain.dw :refer [format-measure]]))

(defn totals-visualization [{:keys [results measures]}]
  (let [grand-total (first results)]
    [:div.ui.statistics
     (doall
       (for [{:keys [name title] :as measure} measures]
         [:div.statistic {:key name}
          [:div.label title]
          [:div.value (format-measure measure grand-total)]]))]))
