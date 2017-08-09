(ns shevek.viewer.visualization
  (:require [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t translation]]
            [shevek.components.drag-and-drop :refer [droppable]]
            [shevek.viewer.visualizations.totals :refer [totals-visualization]]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]
            [shevek.viewer.visualizations.chart :refer [chart-visualization]]))

(defn visualization [viewer]
  (when (get-in viewer [:results :main])
    [:div.visualization
     (if (empty? (viewer :measures))
       [:div.icon-hint
        [:i.warning.circle.icon]
        [:div.text (t :viewer/no-measures)]]
       (let [viztype (get-in viewer [:results :viztype])
             split (get-in viewer [:results :split])]
         (if (and (not= viztype :totals) (empty? split))
           [:div.icon-hint
            [:i.warning.circle.icon]
            [:div.text (t :viewer/split-required (translation :viewer.viztype viztype))]]
           (case viztype
             :totals [totals-visualization viewer]
             :table [table-visualization viewer]
             [chart-visualization viewer]))))]))

(defn visualization-panel []
  [:div.visualization-container.zone.panel.ui.basic.segment
   (merge (droppable #(dispatch :split-replaced %))
          (rpc/loading-class [:results :main]))
   [visualization (db/get :viewer)]])
