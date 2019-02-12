(ns shevek.dashboards.repository
  (:require [shevek.lib.mongodb :as m]
            [shevek.reports.repository :refer [save-report]]
            [com.rpl.specter :refer [transform ALL]]))

; The inverse relation :dashboard-id is used mainly to simplify cascade deletion.
(defn- save-report-and-keep-id [db dashboard-id {:keys [report] :as panel}]
  (if report
    (-> (dissoc panel :report)
        (assoc :report-id (:id (save-report db (assoc report :dashboard-id dashboard-id)))))
    panel))

(defn save-dashboard [db dashboard]
  (let [d (m/save db "dashboards" dashboard)]
    (->> d
         (transform [:panels ALL] (partial save-report-and-keep-id db (:id d)))
         (m/save db "dashboards"))))

(defn delete-dashboard [db id]
  (m/delete-by db "reports" {:dashboard-id id})
  (m/delete-by-id db "dashboards" id))

(defn- fetch-report [db {:keys [report-id] :as panel}]
  (-> (dissoc panel :report-id)
      (assoc :report (m/find-by-id db "reports" report-id))))

(defn find-dashboards [db user-id]
  (m/find-all db "dashboards"
              :where {:owner-id user-id}
              :fields [:name :description :updated-at :owner-id]
              :sort {:name 1}))

(defn delete-dashboards [db user-id]
  (m/delete-by db "dashboards" {:owner-id user-id}))

; This implementation probably is going to be slow if a dashboard has many reports but the alternative that is to use the aggregation framework is less clean and hopefully there won't be many reports per dashboard
(defn find-by-id [db id]
  (->> (m/find-by-id db "dashboards" id)
       (transform [:panels ALL] (partial fetch-report db))))
