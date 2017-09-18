(ns shevek.schema.migrations
  (:require [monger.collection :as mc]
            [monger.operators :refer :all]))

(def migrations
  {:201709141543-remove-pin-in-dashboard
   (fn [db]
     (mc/update db "reports" {} {$unset {:pin-in-dashboard true}} {:multi true}))})
