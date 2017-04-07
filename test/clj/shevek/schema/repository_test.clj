(ns shevek.schema.repository-test
  (:require [clojure.test :refer [is]]
            [shevek.test-helper :refer [spec make]]
            [shevek.schema.repository :as r :refer [Cube]]
            [shevek.db :refer [db]]))

(spec "the cube name should be unique"
  (r/save-cube db (make Cube {:name "C1"}))
  (is (thrown? com.mongodb.DuplicateKeyException (r/save-cube db (make Cube {:name "C1"})))))
