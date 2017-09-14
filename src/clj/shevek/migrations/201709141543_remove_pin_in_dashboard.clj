(ns shevek.migrations.201709141543-remove-pin-in-dashboard
  (:require [monger.collection :as mc]
            [monger.operators :refer :all]))

(defn up [db]
  (mc/update db "reports" {} {$unset {:pin-in-dashboard true}} {:multi true}))
