(ns shevek.dashboard
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.components :refer [page-title]]
            [shevek.dw :as dw]
            [shevek.reports.menu :refer [fetch-reports]]
            [shevek.lib.react :refer [rmap]]))

(defn- cube-card [{:keys [name title description] :or {description (t :cubes/no-desc)}}]
  [:a.cube.card {:on-click #(dispatch :cube-selected name)}
   [:div.content
    [:div.header [:i.cube.icon] title]
    [:div.meta description]]])

(defn- cubes-cards []
  (dw/fetch-cubes)
  (fn []
    (let [cubes (dw/cubes-list)]
       (if (seq cubes)
         [:div.ui.cards (rmap cube-card cubes :name)]
         [:div.tip [:i.info.circle.icon] (t :cubes/missing)]))))

(defn- report-card [{:keys [name description] :as report}]
  [:a.card
   [:div.content
    [:div.header name]
    [:div.meta description]]])

(defn- reports-cards []
  (fetch-reports)
  (fn []
    (let [reports (filter :pin-in-dashboard (db/get :reports))]
      (if (seq reports)
        [:div.ui.two.stackable.cards (rmap report-card reports :name)]
        [:div.tip [:i.info.circle.icon] (t :reports/none)]))))

(defn page []
  [:div#dashboard.ui.container
   [page-title (t :dashboard/title) (t :dashboard/subtitle) "block layout"]
   [:h2.ui.app.header (t :cubes/title)]
   [cubes-cards]
   [:h2.ui.app.header (t :reports/menu)]
   [reports-cards]])
