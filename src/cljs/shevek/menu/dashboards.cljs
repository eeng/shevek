(ns shevek.menu.dashboards
  (:require [reagent.core :as r]
            [cuerdas.core :as str]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.lib.react :refer [rmap]]
            [shevek.home.dashboards :refer [fetch-dashboards select-dashboard]]
            [shevek.navigation :refer [current-page?]]
            [shevek.components.popup :refer [show-popup close-popup]]))

(defn- dashboard-item [{:keys [name description] :as dashboard}]
  [:div.item {:on-click #(do (select-dashboard dashboard) (close-popup))}
   [:div.header name]
   [:div.description description]])

(defn- popup-content []
  (let [dashboards (filter :id (db/get :dashboards))] ; Ignore dashboards that are being created
    (if (seq dashboards)
      [:div.ui.relaxed.middle.aligned.selection.list
       (rmap dashboard-item :id dashboards)]
      [:div (t :errors/no-results)])))

(defn dashboards-menu []
  (when-not (current-page? :home) ; No need to fetch the dashboards again when we are on the home page
    (fetch-dashboards))
  (fn []
    [:a.item {:on-click #(show-popup % popup-content {:position "bottom left"})
              :class (when (current-page? :dashboard) "active")}
     [:i.block.layout.icon] (t :dashboards/title)]))
