(ns shevek.reports.api
  (:require [shevek.reports.repository :as r]
            [shevek.db :refer [db]]))

(defn save-report [{:keys [user]} report]
  (r/save-report db report))

(defn delete-report [_ report]
  (r/delete-report db report))

(defn find-all [_]
  (r/find-reports db))
