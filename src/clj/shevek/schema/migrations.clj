(ns shevek.schema.migrations
  (:require [monger.collection :as mc]
            [monger.operators :refer :all]
            [shevek.lib.time :as t]
            [com.rpl.specter :refer [transform ALL LAST]]))

(def migrations
  {:201709141543-remove-pin-in-dashboard
   (fn [db]
     (mc/update db "reports" {} {$unset {:pin-in-dashboard true}} {:multi true}))

   :201709261644-pivot-tables
   (fn [db]
     (mc/update db "reports" {} {$rename {:split "splits" :filter "filters"}} {:multi true}))

   :201710220155-expand-interval-to-end-of-day
   (fn [db]
     (let [to-end-of-day
           #(t/with-time-zone "America/Argentina/Buenos_Aires"
              ((comp t/to-iso8601 t/end-of-day t/parse-time) %))]
       (doseq [{:keys [_id] :as report} (mc/find-maps db "reports" {:filters.interval {$exists true}})]
         (as-> report r
               (transform [:filters ALL #(:interval %) :interval LAST] to-end-of-day r)
               (select-keys r [:filters])
               (mc/update db "reports" {:_id _id} {$set r})))))

   :20180207170200-owner-id
   (fn [db]
     (mc/update db "reports" {} {$rename {:user-id "owner-id"}} {:multi true})
     (mc/update db "dashboards" {} {$rename {:user-id "owner-id"}} {:multi true}))

   :20180214000000-dashboards-revamp
   (fn [db]
     (mc/update db "dashboards" {} {$rename {:reports "panels"}} {:multi true})
     (mc/update db "dashboards" {:panels {$exists false}} {$set {:panels []}} {:multi true})
     (mc/update db "dashboards" {} {$set {"panels.$[].type" "report"}} {:multi true})

     ; Remove the owner-id from reports that belong to dashboards
     (mc/update db "reports"
                {$where "this['dashboards-ids'] && this['dashboards-ids'].length > 0"}
                {$unset {"owner-id" true}}
                {:multi true})

     ; Convert the many-to-many relation between dashboards an report to a one-to-many
     (let [reports-in-dashs (mc/find-maps db "reports" {$where "this['dashboards-ids'] && this['dashboards-ids'].length > 0"})]
       (doseq [{:keys [_id dashboards-ids] :as report} reports-in-dashs]
         (as-> report r
               (dissoc r :dashboards-ids)
               (assoc r :dashboard-id (first dashboards-ids))
               (mc/update db "reports" {:_id _id} {$set r $unset {"dashboards-ids" true}}))
         (doseq [other-dash-id (rest dashboards-ids)]
           (as-> report r
                 (dissoc r :_id :dashboards-ids)
                 (assoc r :dashboard-id other-dash-id)
                 (mc/save db "reports" r))))))})

#_((migrations :20180214000000-dashboards-revamp) shevek.db/db)
#_(let [db shevek.db/db])
