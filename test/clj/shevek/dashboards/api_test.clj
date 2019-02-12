(ns shevek.dashboards.api-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.dashboards.api :as api]
            [shevek.makers :refer [make make!]]
            [shevek.schemas.user :refer [User]]
            [shevek.schemas.dashboard :refer [Dashboard]]))

(use-fixtures :once wrap-unit-tests)

(deftest authorization-test
  (testing "save"
    (it "create should set the owner"
      (let [u1 (:id (make! User))]
        (is (= u1 (:owner-id (api/save {:user-id u1} (make Dashboard)))))))

    (it "update can only be made by the owner"
      (let [u1 (:id (make! User))
            u2 (:id (make! User))
            d (make! Dashboard {:owner-id u1})]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized"
                              (api/save {:user-id u2} d))))))

  (testing "find-all"
    (it "should return the user's dashboards only"
      (let [u1 (:id (make! User))
            u2 (:id (make! User))
            d1 (:id (make! Dashboard {:owner-id u1}))
            d2 (:id (make! Dashboard {:owner-id u2}))]
        (is (= [d1] (map :id (api/find-all {:user-id u1}))))
        (is (= [d2] (map :id (api/find-all {:user-id u2}))))))))
