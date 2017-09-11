(ns shevek.dashboards.repository
  (:require [schema.core :as s]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [monger.collection :as mc]
            [monger.query :as mq]
            [shevek.lib.mongodb :refer [timestamp]]))

(s/defn save-dashboard [db dashboard :- Dashboard]
  (mc/save-and-return db "dashboards" (timestamp dashboard)))

(s/defn delete-dashboard [db {:keys [_id]}]
  (mc/remove-by-id db "dashboards" _id)
  true)

(defn find-dashboards [db user-id]
  (mq/with-collection db "dashboards"
    (mq/find {:user-id user-id})
    (mq/sort {:name 1})))

(s/defn delete-dashboards [db user-id]
  (mc/remove db "dashboards" {:user-id user-id}))
