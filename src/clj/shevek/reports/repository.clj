(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.schemas.report :refer [Report]]
            [shevek.lib.mongodb :as m]))

(s/defn save-report [db report :- Report]
  (m/save db "reports" report))

(s/defn delete-report [db {:keys [id]}]
  (m/delete-by-id db "reports" id))

(defn find-reports [db user-id]
  (m/find-all db "reports" :where {:user-id user-id} :sort {:name 1}))

(s/defn delete-reports [db user-id]
  (m/delete-by db "reports" {:user-id user-id}))
