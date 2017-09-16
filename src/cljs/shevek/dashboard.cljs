(ns shevek.dashboard
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.components.text :refer [page-title]]))

(defevh :dashboard-selected [db id]
  (rpc/fetch db :dashboard "dashboards.api/find-by-id" :args [id])
  (dispatch :navigate :dashboard)
  (dissoc db :dashboard))

(defn page []
  (if-let [{:keys [name description] :as dashboard} (db/get :dashboard)]
    [:div#dashboard.ui.container
     [page-title name description "block layout"]]
    [:div.ui.active.loader]))
