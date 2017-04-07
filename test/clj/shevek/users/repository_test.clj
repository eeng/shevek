(ns shevek.users.repository-test
  (:require [clojure.test :refer [is]]
            [shevek.test-helper :refer [spec]]
            [shevek.makers :refer [make!]]
            [shevek.users.repository :refer [User]]
            [shevek.db :refer [db]]))

(spec "the username should be unique"
  (make! User {:username "ddchp"})
  (is (thrown? com.mongodb.DuplicateKeyException (make! User {:username "ddchp"}))))
