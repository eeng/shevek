(ns shevek.schema.test-migrations.1-add-first-record
  (:require [monger.collection :as mc]
            [shevek.makers :refer [make]]
            [shevek.schemas.report :refer [Report]]))

(defn up [db]
  (mc/insert db "reports" (make Report {:name "R1"})))
