(ns shevek.viewer.viztype
  (:require [shevek.components.popup :refer [show-popup close-popup popup-opened?]]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [translation]]
            [shevek.viewer.url :refer [store-viewer-in-url]]))

(defevhi :viztype-changed [db viztype]
  {:after [close-popup store-viewer-in-url]}
  (-> db
      (assoc-in [:viewer :viztype] viztype)
      (assoc-in [:viewer :visualization :viztype] viztype)
      (cond->
       (= viztype :totals) (assoc-in [:viewer :split] []))))

(def viztype-icons {:totals "slack"
                    :table "table"
                    :bar-chart "bar chart"
                    :line-chart "line chart"
                    :pie-chart "pie chart"})

(defn- viztype-button [viztype]
  [:div.viztype-button
   [:i.icon {:class (viztype-icons viztype)}]
   (translation :viewer.viztype viztype)])

(defn- viztype-popup []
  [:div.viztype-popup
   (for [[viztype _] viztype-icons]
     [:a {:key viztype :on-click #(dispatch :viztype-changed viztype)}
      [viztype-button viztype]])])

(defn viztype-selector []
  (let [viewer (db/get :viewer)]
    [:div.viztype-selector.panel {:on-click #(show-popup % [viztype-popup] {:position "bottom right" :id :viztype})
                                  :class (when (popup-opened? :viztype) "active")}
     [viztype-button (viewer :viztype)]]))
