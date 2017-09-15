(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.schemas.report :refer [Report]]
            [shevek.lib.mongodb :as m]
            [monger.collection :as mc]
            [monger.operators :refer :all]))

(defn- remove-report-from-non-selected-dashboards [db rid ds-ids]
  (mc/update db "dashboards"
             (cond-> {"reports.report-id" rid}
                     ds-ids (assoc :_id {$nin ds-ids}))
             {$pull {:reports {:report-id rid}}}
             {:multi true}))

(defn- add-report-to-selected-dashboards [db rid ds-ids]
  (when (seq ds-ids)
    (mc/update db "dashboards"
               {:_id {$in ds-ids} "reports.report-id" {$ne rid}}
               {$push {:reports {:report-id rid}}}
               {:multi true})))

(s/defn save-report [db {:keys [dashboards-ids] :as report :- Report}]
  (let [r (m/save db "reports" report)
        rid (m/oid (:id r))
        ds-ids (map m/oid dashboards-ids)]
    (remove-report-from-non-selected-dashboards db rid ds-ids)
    (add-report-to-selected-dashboards db rid ds-ids)
    r))

(s/defn delete-report [db {:keys [id]}]
  (m/delete-by-id db "reports" id))

(defn find-reports [db user-id]
  (m/find-all db "reports" :where {:user-id user-id} :sort {:name 1}))

(s/defn delete-reports [db user-id]
  (m/delete-by db "reports" {:user-id user-id}))
