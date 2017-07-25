(ns shevek.users.api-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [it]]
            [shevek.users.api :as api]))

(deftest authorization-test
  (it "only admins can list users"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized" (api/find-all {})))
    (is (= [] (api/find-all {:user {:admin true}})))))
