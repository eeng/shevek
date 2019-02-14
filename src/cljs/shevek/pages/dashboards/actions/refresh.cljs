(ns shevek.pages.dashboards.actions.refresh
  (:require [shevek.rpc :as rpc]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.pages.designer.actions.refresh :refer [refresh-button] :rename {refresh-button designer-refresh-button}]))

(defevh :dashboard/refresh [db]
  (when-not (rpc/loading?) ; Do not refresh again if there is a slow query still running
    (doseq [{:keys [id report]} (get-in db [:current-dashboard :panels])]
      (dispatch :dashboard/report-query report id)))
  db)

(defn refresh-button []
  [designer-refresh-button {:on-refresh #(dispatch :dashboard/refresh)}])
