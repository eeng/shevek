(ns shevek.pages.designer.viztype
  (:require [reagent.core :as r]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [translation]]
            [shevek.pages.designer.helpers :refer [notify-designer-changes]]))

(defevhi :designer/viztype-changed [db viztype]
  {:after [close-popup notify-designer-changes]}
  (-> db
      (assoc-in [:designer :viztype] viztype)
      (cond->
       (= viztype :totals) (assoc-in [:designer :splits] []))))

(def viztype-icons {:totals "slack"
                    :table "table"
                    :bar-chart "bar chart"
                    :line-chart "line chart"
                    :pie-chart "pie chart"})

(defn- viztype-button [viztype]
  [:div.viztype-button
   [:i.icon {:class (viztype-icons viztype)}]
   (translation :designer.viztype viztype)])

(defn- viztype-popup []
  [:div.viztype-popup
   (for [[viztype _] viztype-icons]
     [:a {:key viztype :on-click #(dispatch :designer/viztype-changed viztype)}
      [viztype-button viztype]])])

(defn viztype-selector []
  (let [opened (r/atom false)]
    (fn [viztype]
      [:div.viztype-selector.panel {:on-click #(show-popup % [viztype-popup] {:position "bottom right"
                                                                              :on-toggle (partial reset! opened)})
                                    :class (when @opened "active")}
       [viztype-button viztype]])))
