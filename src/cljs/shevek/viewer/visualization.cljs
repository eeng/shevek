(ns shevek.viewer.visualization
  (:require [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t translation]]
            [shevek.components.text :refer [warning]]
            [shevek.components.drag-and-drop :refer [droppable]]
            [shevek.viewer.visualizations.totals :refer [totals-visualization]]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]
            [shevek.viewer.visualizations.chart :refer [chart-visualization]]
            [shevek.schemas.conversion :refer [splits]]))

(defn visualization [{:keys [results measures viztype] :as viz}]
  (when results ; Results are nil until first query finish
    [:div.visualization
     (cond
       (empty? measures)
       [warning (t :viewer/no-measures)]

       (and (empty? results) (seq (splits viz)))
       [warning (t :viewer/no-results)]

       (and (not= viztype :totals) (empty? (splits viz)))
       [warning (t :viewer/split-required (translation :viewer.viztype viztype))]

       :else
       (case viztype
         :totals [totals-visualization viz]
         :table [table-visualization viz]
         [chart-visualization viz]))]))

(defn visualization-panel []
  [:div.visualization-container.zone.panel.ui.basic.segment
   (merge (droppable #(dispatch :split-replaced %))
          (rpc/loading-class [:viewer :visualization]))
   [visualization (db/get-in [:viewer :visualization])]])
