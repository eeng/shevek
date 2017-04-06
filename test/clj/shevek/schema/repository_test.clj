(ns shevek.schema.repository-test
  (:require [clojure.test :refer [is]]
            [shevek.test-helper :refer [spec]]
            [shevek.schema.repository :as r]
            [shevek.db :refer [db]]))

(spec "the cube name should be unique"
  (r/save-cube db {:name "C1"})
  (is (thrown? com.mongodb.DuplicateKeyException (r/save-cube db {:name "C1"}))))
