(ns shevek.lib.rpc-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [it]]
            [shevek.lib.rpc :as rpc]))

(deftest call-fn-tests
  (it "allows to functions"
    (is (= [] (rpc/call-fn {:params {:fn "users.api/find-all"}}))))

  (testing "only apis namespaces are allowed"
    (is (thrown? AssertionError (rpc/call-fn {:params {:fn "users.repository/find-users"}})))))
