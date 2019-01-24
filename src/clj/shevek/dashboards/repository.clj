(ns shevek.dashboards.repository
  (:require [shevek.lib.mongodb :as m]
            [monger.collection :as mc]
            [monger.operators :refer [$pull]]
            [shevek.reports.repository :refer [save-report]]
            [com.rpl.specter :refer [transform ALL]]))

(defn- save-report-and-keep-id [db {:keys [report] :as panel}]
  (if report
    (-> (dissoc panel :report)
        (assoc :report-id (:id (save-report db report))))
    panel))

(defn save-dashboard [db dashboard]
  (->> (m/save db "dashboards" dashboard)
       (transform [:panels ALL] (partial save-report-and-keep-id db))
       (m/save db "dashboards")))

(defn delete-dashboard [db {:keys [id]}]
  (mc/update db "reports" {:dashboards-ids (m/oid id)} {$pull {:dashboards-ids (m/oid id)}} {:multi true})
  (m/delete-by-id db "dashboards" id))

(defn- fetch-report [db {:keys [report-id] :as panel}]
  (-> (dissoc panel :report-id)
      (assoc :report (m/find-by-id db "reports" report-id))))

(defn find-dashboards [db user-id]
  (m/find-all db "dashboards"
              :where {:user-id user-id}
              :fields [:name :updated-at :user-id]
              :sort {:name 1}))

(defn delete-dashboards [db user-id]
  (m/delete-by db "dashboards" {:user-id user-id}))

; This implementation probably is going to be slow if a dashboard has many reports but the alternative that is to use the aggregation framework is less clean and hopefully there won't be many reports per dashboard
(defn find-by-id [db id]
  (->> (m/find-by-id db "dashboards" id)
       (transform [:panels ALL] (partial fetch-report db))))
