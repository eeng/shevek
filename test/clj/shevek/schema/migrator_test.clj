(ns shevek.schema.migrator-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [it]]
            [monger.collection :as mc]
            [shevek.db :refer [db]]
            [shevek.schema.migrator :refer [migrate!]]))

(deftest migrate-tests
  (it "should run the up function of all migration files in the directory"
    (mc/remove db "schema-migrations")
    (migrate! db "shevek/schema/test_migrations")
    (is (= ["R1" "R2"] (map :name (mc/find-maps db "reports")))))

  (it "each migration should be run only once"
    (mc/remove db "schema-migrations")
    (mc/insert db "schema-migrations" {:version "1"})
    (migrate! db "shevek/schema/test_migrations")
    (is (= ["R2"] (map :name (mc/find-maps db "reports"))))
    (is (= 2 (mc/count db "schema-migrations"))))

  (it "should ignore files that are not migrations"
    (mc/remove db "schema-migrations")
    (migrate! db "shevek/support")
    (is (= 0 (mc/count db "schema-migrations")))))
