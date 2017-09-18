(ns shevek.schema.migrator-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [it]]
            [monger.collection :as mc]
            [shevek.db :refer [db]]
            [shevek.schema.migrator :refer [migrate!]]
            [shevek.schema.test-migrations :refer [test-migrations]]))

(deftest migrate-tests
  (it "should run the up function of all migration files in the directory"
    (mc/remove db "schema-migrations")
    (migrate! db test-migrations)
    (is (= ["R1" "R2"] (map :name (mc/find-maps db "reports")))))

  (it "each migration should be run only once"
    (mc/remove db "schema-migrations")
    (mc/insert db "schema-migrations" {:name "1-add-first-record"})
    (migrate! db test-migrations)
    (is (= ["R2"] (map :name (mc/find-maps db "reports"))))
    (is (= 2 (mc/count db "schema-migrations")))))
