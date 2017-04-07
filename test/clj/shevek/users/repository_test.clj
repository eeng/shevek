(ns shevek.users.repository-test
  (:require [clojure.test :refer [is]]
            [shevek.test-helper :refer [spec]]
            [shevek.makers :refer [make!]]
            [shevek.users.repository :refer [User find-all]]
            [shevek.db :refer [db]]))

(spec "the username should be unique"
  (make! User {:username "ddchp"})
  (is (thrown? com.mongodb.DuplicateKeyException (make! User {:username "ddchp"}))))

(spec "find-all should return users sorted by username"
  (make! User {:username "a"})
  (make! User {:username "c"})
  (make! User {:username "b"})
  (is (= ["a" "b" "c"] (map :username (find-all db)))))
