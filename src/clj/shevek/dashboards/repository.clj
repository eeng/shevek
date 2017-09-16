(ns shevek.dashboards.repository
  (:require [schema.core :as s]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.lib.mongodb :as m]
            [com.rpl.specter :refer [transform ALL]]))

(s/defn save-dashboard [db dashboard :- Dashboard]
  (m/save db "dashboards" dashboard))

(s/defn delete-dashboard [db {:keys [id]}]
  (m/delete-by-id db "dashboards" id))

(defn- fetch-report [db {:keys [report-id] :as r}]
  (assoc r :report (m/find-by-id db "reports" report-id)))

(defn find-dashboards [db user-id]
  (m/find-all db "dashboards" :where {:user-id user-id} :sort {:name 1}))

(s/defn delete-dashboards [db user-id]
  (m/delete-by db "dashboards" {:user-id user-id}))

; This implementation probably is going to be slow if a dashboard has many reports but the alternative that is to use the aggregation framework is less clean and hopefully there won't be many reports per dashboard
(defn find-by-id [db id]
  (->> (m/find-by-id db "dashboards" id)
       (transform [:reports ALL] (partial fetch-report db))))
