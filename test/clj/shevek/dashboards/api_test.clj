(ns shevek.dashboards.api-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [it]]
            [shevek.asserts :refer [submap?]]
            [shevek.dashboards.api :as api]
            [shevek.makers :refer [make!]]
            [shevek.schemas.user :refer [User]]
            [shevek.schemas.dashboard :refer [Dashboard]]))

(deftest authorization-test
  (it "only the author of a dashboard can view it"
    (let [u1 (:id (make! User))
          u2 (:id (make! User))
          d (:id (make! Dashboard {:user-id u1}))]
      (is (submap? {:id d} (api/find-by-id {:user-id u1} d)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized" (api/find-by-id {:user-id u2} d))))))
