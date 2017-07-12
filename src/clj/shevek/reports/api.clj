(ns shevek.reports.api
  (:require [shevek.reports.repository :as r]
            [shevek.db :refer [db]]))

(defn save-report [{:keys [user-id]} report]
  (r/save-report db (assoc report :user-id user-id)))

(defn delete-report [_ report]
  (r/delete-report db report))

(defn find-all [{:keys [user-id]}]
  (r/find-reports db user-id))
