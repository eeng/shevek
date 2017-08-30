(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.schemas.report :refer [Report]]
            [monger.collection :as mc]
            [monger.query :as mq]
            [shevek.lib.mongodb :refer [timestamp]]))

(s/defn save-report [db report :- Report]
  (mc/save-and-return db "reports" (timestamp report)))

(s/defn delete-report [db {:keys [_id]}]
  (mc/remove-by-id db "reports" _id)
  true)

(defn find-reports [db user-id]
  (mq/with-collection db "reports"
    (mq/find {:user-id user-id})
    (mq/sort {:name 1})))

(s/defn delete-reports [db user-id]
  (mc/remove db "reports" {:user-id user-id}))
