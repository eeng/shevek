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
     (mc/update db "dashboards" {} {$rename {:user-id "owner-id"}} {:multi true}))})

#_((migrations :201710220155-expand-interval-to-end-of-day) shevek.db/db)
