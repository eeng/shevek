(ns shevek.dashboard
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.lib.react :refer [rmap]]
            [shevek.lib.dw.cubes :refer [set-cube-defaults]]
            [shevek.schemas.conversion :refer [report->viewer]]
            [shevek.components.text :refer [page-title warning loader]]
            [shevek.viewer.visualization :refer [visualization]]
            [shevek.viewer.shared :refer [send-visualization-query cube-authorized?]]))

(defevh :dashboard/refresh [db]
  (console.log "TODO dashboard refresh")
  db)

(defn- reports-ids-as-keys [db dashboard]
  (assoc db :dashboard (update dashboard :reports #(zipmap (map :id %) %))))

(defevh :dashboard-selected [db id]
  (rpc/fetch db :dashboard "dashboards/find-by-id" :args [id] :handler reports-ids-as-keys)
  (dispatch :navigate :dashboard)
  (dissoc db :dashboard))

(defn vis-key [{:keys [id]}]
  [:dashboard :reports id :visualization])

(defn- cube-arrived [db cube report]
  (let [viewer (report->viewer report (set-cube-defaults cube))]
    (if (cube-authorized? viewer)
      (send-visualization-query db viewer (vis-key report))
      (rpc/loaded db (vis-key report)))))

(defevh :dashboard/cube-requested [db {:keys [cube] :as report}]
  (rpc/fetch db :cube-metadata "schema/cube" :args [cube] :handler #(cube-arrived %1 %2 report)))

(defn- report-card [{:keys [name description] :as report}]
  (dispatch :dashboard/cube-requested report)
  (fn []
    [:div.report.card
     [:div.content {:on-click #(dispatch :report-selected report)}
      [:div.header name]
      [:div.meta description]]
     [:div.content.visualization-container
      [loader (vis-key report)]
      (if-let [vis (db/get-in (vis-key report))]
        (if (cube-authorized? vis)
          [visualization vis]
          [warning (t :reports/unauthorized)]))]]))

(defn- reports-cards [reports]
  [:div.ui.two.stackable.cards (rmap report-card :id reports)])

(defn page []
  (if-let [{:keys [name description reports] :as dashboard} (db/get :dashboard)]
    [:div#dashboard.ui.container
     [page-title name description "block layout"]
     (if (seq reports)
       [reports-cards (vals reports)]
       [warning (t :dashboards/no-reports)])]
    [:div.ui.active.loader]))
