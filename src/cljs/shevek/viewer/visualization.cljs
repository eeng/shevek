(ns shevek.viewer.visualization
  (:require [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t translation]]
            [shevek.components.text :refer [warning loader]]
            [shevek.components.drag-and-drop :refer [droppable]]
            [shevek.viewer.visualizations.totals :refer [totals-visualization]]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]
            [shevek.viewer.visualizations.chart :refer [chart-visualization]]))

(defn visualization [{:keys [results measures viztype splits] :as viz}]
  (when results ; Results are nil until first query finish
    [:div.visualization
     (cond
       (empty? measures)
       [warning (t :viewer/no-measures)]

       (and (empty? results) (seq splits))
       [warning (t :viewer/no-results)]

       (and (not= viztype :totals) (empty? splits))
       [warning (t :viewer/split-required (translation :viewer.viztype viztype))]

       :else
       (case viztype
         :totals [totals-visualization viz]
         :table [table-visualization viz]
         [chart-visualization viz]))]))

(defn visualization-panel []
  [:div.visualization-container.ui.basic.segment
   (merge (droppable #(dispatch :split-replaced %)))
   [loader [:viewer :visualization]]
   [visualization (db/get-in [:viewer :visualization])]])
