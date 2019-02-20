(ns shevek.dashboards.repository
  (:require [shevek.lib.mongodb :as m]
            [shevek.reports.repository :refer [save-report]]
            [com.rpl.specter :refer [transform ALL NIL->VECTOR]]))

; The inverse relation :dashboard-id is used mainly to simplify cascade deletion.
(defn- save-report-and-keep-id [db dashboard-id {:keys [report] :as panel}]
  (if report
    (-> (dissoc panel :report)
        (assoc :report-id (:id (save-report db (assoc report :dashboard-id dashboard-id)))))
    panel))

(defn save-dashboard [db dashboard]
  (let [d (m/save db "dashboards" dashboard)]
    (->> d
         (transform [:panels NIL->VECTOR ALL] (partial save-report-and-keep-id db (:id d)))
         (m/save db "dashboards"))))

(defn find-dashboards [db user-id]
  (m/find-all db "dashboards"
              :where {:owner-id user-id}
              :fields [:name :description :updated-at :owner-id]
              :sort {:name 1}))

(defn find-by-id [db id]
  (m/find-by-id db "dashboards" id))

(defn- fetch-report [db {:keys [report-id] :as panel}]
  (let [report-copy (-> (m/find-by-id db "reports" report-id)
                        (dissoc :id))]
    (-> (dissoc panel :report-id)
        (assoc :report report-copy))))

(defn- merge-master-if-slave [db {:keys [master-id] :as dashboard}]
  (if master-id
    (let [master (find-by-id db master-id)]
      (assert master)
      (assoc dashboard :panels (master :panels)))
    dashboard))

; This implementation probably is going to be slow if a dashboard has many reports but the alternative that is to use the aggregation framework is less clean and hopefully there won't be many reports per dashboard
(defn find-with-relations [db id]
  (when-let [d (find-by-id db id)]
    (->> (merge-master-if-slave db d)
         (transform [:panels ALL] (partial fetch-report db)))))

(defn upgrade-slaves-of [db id]
  (doseq [slave (m/find-all db "dashboards" :where {:master-id id})]
    (as-> slave s
          (find-with-relations db (:id s))
          (dissoc s :master-id)
          (save-dashboard db s))))

(defn delete-dashboard [db id]
  (upgrade-slaves-of db id)
  (m/delete-by db "reports" {:dashboard-id id})
  (m/delete-by-id db "dashboards" id))

(defn delete-dashboards [db user-id]
  (doseq [{:keys [id]} (find-dashboards db user-id)]
    (delete-dashboard db id)))
