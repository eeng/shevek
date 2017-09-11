(ns shevek.dashboards.api
  (:require [shevek.dashboards.repository :as r]
            [shevek.db :refer [db]]))

(defn save-dashboard [{:keys [user-id]} dashboard]
  (r/save-dashboard db (assoc dashboard :user-id user-id)))

(defn delete-dashboard [_ dashboard]
  (r/delete-dashboard db dashboard))

(defn find-all [{:keys [user-id]}]
  (r/find-dashboards db user-id))
