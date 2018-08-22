(ns shevek.users.api-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer :all]
            [shevek.users.api :as api]
            [cprop.core :refer [load-config]]))

(use-fixtures :once wrap-unit-tests)

(deftest authorization-test
  (it "only admins can list users"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized" (api/find-all {})))
    (is (= [] (api/find-all {:user {:admin true}})))))
