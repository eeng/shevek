(ns shevek.pages.dashboards.actions.refresh
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.refresh :as refresh]
            [shevek.lib.time :refer [now]]))

(defevh :dashboard/refresh [db]
  (assoc-in db [:current-dashboard :last-refresh-at] (now)))

(defn refresh-button []
  [refresh/refresh-button {:on-refresh #(dispatch :dashboard/refresh)}])
