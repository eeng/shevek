(ns shevek.viewer.visualization
  (:require [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t translation]]
            [shevek.components.drag-and-drop :refer [droppable]]
            [shevek.viewer.visualizations.totals :refer [totals-visualization]]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]
            [shevek.viewer.visualizations.chart :refer [chart-visualization]]))

(defn visualization [{:keys [results measures viztype split] :as viz}]
  (when results
    [:div.visualization
     (if (empty? measures)
       [:div.icon-hint
        [:i.warning.circle.icon]
        [:div.text (t :viewer/no-measures)]]
       (if (and (not= viztype :totals) (empty? split))
         [:div.icon-hint
          [:i.warning.circle.icon]
          [:div.text (t :viewer/split-required (translation :viewer.viztype viztype))]]
         (case viztype
           :totals [totals-visualization viz]
           :table [table-visualization viz]
           [chart-visualization viz])))]))

(defn visualization-panel []
  [:div.visualization-container.zone.panel.ui.basic.segment
   (merge (droppable #(dispatch :split-replaced %))
          (rpc/loading-class [:viewer :visualization]))
   [visualization (db/get-in [:viewer :visualization])]])
