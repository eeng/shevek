(ns shevek.pages.designer.visualization
  (:require [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t translation]]
            [shevek.components.text :refer [loader]]
            [shevek.components.message :refer [warning]]
            [shevek.components.drag-and-drop :refer [droppable]]
            [shevek.pages.cubes.helpers :refer [cube-authorized?]]
            [shevek.pages.designer.helpers :refer [build-visualization]]
            [shevek.pages.designer.visualizations.totals :refer [totals-visualization]]
            [shevek.pages.designer.visualizations.pivot-table :refer [table-visualization]]
            [shevek.pages.designer.visualizations.chart :refer [chart-visualization]]))

(defn refreshing-indicator [{:keys [refreshing?]}]
  (when refreshing?
    [:div.ui.right.corner.label
     [:i.sync.loading.icon]]))

(defn visualization [results {:keys [cube] :as report} & [opts]]
  (when results ; Results are nil until query finish, and the cube could not exists yet when the user loads the page
    (if (cube-authorized? cube)
      (let [{:keys [results measures viztype splits] :as viz} (build-visualization results report)]
        [:div.visualization {:class viztype}
         [refreshing-indicator opts]

         (cond
           (empty? measures)
           [warning (t :designer/no-measures)]

           (and (empty? results) (seq splits))
           [warning (t :designer/no-results)]

           (and (not= viztype :totals) (empty? splits))
           [warning (t :designer/split-required (translation :designer.viztype viztype))]

           :else
           (case viztype
             :totals [totals-visualization viz]
             :table [table-visualization viz]
             [chart-visualization viz]))])
      [:div.visualization.warning
       [warning (t :cubes/unauthorized)]])))

(defn visualization-panel [report report-results]
  [:div.visualization-container.ui.basic.segment
   (merge (droppable #(dispatch :designer/split-replaced %)))
   [loader [:designer :report-results]]
   [visualization report-results report]])
