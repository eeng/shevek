(ns shevek.pages.dashboards.actions.refresh
  (:require [shevek.rpc :as rpc]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.refresh :as refresh]))

(defevh :dashboard/refresh [db]
  (when-not (rpc/loading?) ; Do not refresh again if there is a slow query still running
    (doseq [{:keys [id report type]} (get-in db [:current-dashboard :panels])
            :when (= type "report")]
      (dispatch :dashboard/report-query report id)))
  db)

(defn refresh-button []
  [refresh/refresh-button {:on-refresh #(dispatch :dashboard/refresh)}])
