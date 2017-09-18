(ns shevek.dashboard
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.components.text :refer [page-title warning]]))

(defevh :dashboard-selected [db id]
  (rpc/fetch db :dashboard "dashboards.api/find-by-id" :args [id])
  (dispatch :navigate :dashboard)
  (dissoc db :dashboard))

(defevh :dashboard/refresh [db]
  (console.log "TODO dashboard refresh")
  db)

(defn- reports-cards [reports]
  [:div "cards"])

(defn page []
  (if-let [{:keys [name description reports] :as dashboard} (db/get :dashboard)]
    [:div#dashboard.ui.container
     [page-title name description "block layout"]
     (if (seq reports)
       [reports-cards reports]
       [warning (t :dashboards/no-reports)])]
    [:div.ui.active.loader]))
