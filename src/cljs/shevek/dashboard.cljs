(ns shevek.dashboard
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.rpc :as rpc]
            [shevek.components :refer [page-title]]
            [shevek.dw :as dw]
            [shevek.reports.menu :refer [fetch-reports]]
            [shevek.schemas.conversion :refer [report->query]]
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

(defevh :dashboard/query-executed [db results results-key]
  (-> (assoc-in db results-key results)
      (rpc/loaded results-key)))

(defevh :dashboard/cube-arrived [db report cube results-key]
  (let [q (report->query report (dw/set-cube-defaults cube))]
    (rpc/call "querying.api/query" :args [q] :handler #(dispatch :dashboard/query-executed % results-key))
    db))

(defevh :dashboard/cube-requested [db {:keys [name cube] :as report}]
  (let [results-key [:dashboard :reports name]]
    (rpc/call "schema.api/cube" :args [cube] :handler #(dispatch :dashboard/cube-arrived report % results-key))
    (rpc/loading db results-key)))

(defn- report-card [{:keys [name description] :as report}]
  (dispatch :dashboard/cube-requested report)
  (fn []
    [:a.card
     [:div.content
      [:div.header name]
      [:div.meta description]]]))

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
   [:h2.ui.app.header (t :reports/pinned)]
   [reports-cards]])
