(ns shevek.schema.test-migrations
  (:require [monger.collection :as mc]
            [shevek.makers :refer [make]]
            [shevek.schemas.report :refer [Report]]))

(def test-migrations
  {:1-add-first-record
   (fn [db]
     (mc/insert db "reports" (make Report {:name "R1"})))

   :2-add-second-record
   (fn [db]
     (mc/insert db "reports" (make Report {:name "R2"})))})
