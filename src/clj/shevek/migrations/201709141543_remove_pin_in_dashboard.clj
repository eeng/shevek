(ns shevek.migrations.201709141543-remove-pin-in-dashboard
  (:require [monger.collection :as mc]))

(defn up [db]
  (mc/update db "reports" {} {monger.operators/$unset {:pin-in-dashboard true}} {:multi true}))
