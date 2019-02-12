(ns shevek.dashboards.api-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.asserts :refer [submap?]]
            [shevek.dashboards.api :as api]
            [shevek.makers :refer [make make!]]
            [shevek.schemas.user :refer [User]]
            [shevek.schemas.dashboard :refer [Dashboard]]))

(use-fixtures :once wrap-unit-tests)

(deftest authorization-test
  (testing "save"
    (it "should set the owner"
      (let [u1 (:id (make! User))]
        (is (= u1 (:owner-id (api/save {:user-id u1} (make Dashboard))))))))

  (testing "find-by-id"
    (it "only the author of a dashboard can view it"
      (let [u1 (:id (make! User))
            u2 (:id (make! User))
            d (:id (make! Dashboard {:owner-id u1}))]
        (is (submap? {:id d} (api/find-by-id {:user-id u1} d)))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized" (api/find-by-id {:user-id u2} d)))))))
