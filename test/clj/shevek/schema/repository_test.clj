(ns shevek.schema.repository-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer :all]
            [shevek.makers :refer [make!]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.db :refer [db]]))

(use-fixtures :once wrap-unit-tests)

(deftest save-cube-tests
  (it "the cube name should be unique"
    (make! Cube {:name "C1"})
    (is (thrown? com.mongodb.DuplicateKeyException (make! Cube {:name "C1"})))))
