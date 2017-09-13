(ns shevek.dashboards.repository
  (:require [schema.core :as s]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.lib.mongodb :as m]))

(s/defn save-dashboard [db dashboard :- Dashboard]
  (m/save db "dashboards" dashboard))

(s/defn delete-dashboard [db {:keys [id]}]
  (m/delete-by-id db "dashboards" id))

(defn find-dashboards [db user-id]
  (m/find-all db "dashboards" :where {:user-id user-id} :sort {:name 1}))

(s/defn delete-dashboards [db user-id]
  (m/delete-by db "dashboards" {:user-id user-id}))
