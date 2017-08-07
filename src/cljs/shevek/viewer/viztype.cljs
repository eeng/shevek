(ns shevek.viewer.viztype
  (:require-macros [shevek.reflow.macros :refer [defevhi]])
  (:require [shevek.components.popup :refer [show-popup close-popup popup-opened?]]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [translation]]))

(defevhi :viztype-changed [db viztype]
  {:after [close-popup]}
  (assoc-in db [:viewer :viztype] viztype))

(def viztype-icons {"totals" "slack"
                    "table" "table"
                    "bar-chart" "bar chart"
                    "line-chart" "line chart"
                    "pie-chart" "pie chart"})

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
