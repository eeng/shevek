(ns shevek.reports.repository
  (:require [shevek.lib.mongodb :as m]
            [monger.collection :as mc]
            [monger.operators :refer :all]))

(defn- remove-report-from-non-selected-dashboards [db rid ds-ids]
  (mc/update db "dashboards"
             (cond-> {"panels.report-id" rid}
                     ds-ids (assoc :_id {$nin ds-ids}))
             {$pull {:panels {:report-id rid}}}
             {:multi true}))

(defn- add-report-to-selected-dashboards [db rid ds-ids]
  (when (seq ds-ids)
    (mc/update db "dashboards"
               {:_id {$in ds-ids} "panels.report-id" {$ne rid}}
               {$push {:panels {:report-id rid}}}
               {:multi true})))

; TODO DASHBOARD este hacia mas cosas antes, se encargaba de mantener la relacion inversa con los dashboards, revisar
(defn save-report [db report]
  (m/save db "reports" report))

(defn delete-report [db {:keys [id]}]
  (remove-report-from-non-selected-dashboards db (m/oid id) [])
  (m/delete-by-id db "reports" id))

(defn find-reports [db user-id]
  (m/find-all db "reports" :where {:user-id user-id} :sort {:name 1}))

(defn delete-reports [db user-id]
  (m/delete-by db "reports" {:user-id user-id}))

(defn find-by-id [db id]
  (m/find-by-id db "reports" id))
