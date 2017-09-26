(ns shevek.schema.migrations
  (:require [monger.collection :as mc]
            [monger.operators :refer :all]))

(def migrations
  {:201709141543-remove-pin-in-dashboard
   (fn [db]
     (mc/update db "reports" {} {$unset {:pin-in-dashboard true}} {:multi true}))

   :201709261644-pivot-tables
   (fn [db]
     (mc/update db "reports" {} {$rename {:split "splits" :filter "filters"}} {:multi true}))})
