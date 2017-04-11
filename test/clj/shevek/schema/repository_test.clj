(ns shevek.schema.repository-test
  (:require [clojure.test :refer [is]]
            [shevek.test-helper :refer [spec]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.db :refer [db]]))

(spec "the cube name should be unique"
  (make! Cube {:name "C1"})
  (is (thrown? com.mongodb.DuplicateKeyException (make! Cube {:name "C1"}))))
