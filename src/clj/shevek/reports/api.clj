(ns shevek.reports.api
  (:require [shevek.reports.repository :as r]
            [shevek.db :refer [db]]
            [schema.core :as s]
            [shevek.schemas.report :refer [Report]]))

(s/defn save-report [{:keys [user-id]} report :- Report]
  (r/save-report db (assoc report :user-id user-id)))

(defn delete-report [_ report]
  (r/delete-report db report))

(defn find-all [{:keys [user-id]}]
  (r/find-reports db user-id))

; TODO DASHBOARD autorizar?
(defn find-by-id [_ id]
  (r/find-by-id db id))
