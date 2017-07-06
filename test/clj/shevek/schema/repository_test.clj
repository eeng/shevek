(ns shevek.schema.repository-test
  (:require [clojure.test :refer [deftest is]]
            [shevek.test-helper :refer [it]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.db :refer [db]]))

(deftest save-cube-tests
  (it "the cube name should be unique"
    (make! Cube {:name "C1"})
    (is (thrown? com.mongodb.DuplicateKeyException (make! Cube {:name "C1"})))))
