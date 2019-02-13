(ns shevek.dashboards.api-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.dashboards.api :as api]
            [shevek.makers :refer [make make!]]
            [shevek.asserts :refer [submap? submaps?]]
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

  (testing "import"
    (it "creates a slave dashboard that reference a master one",
      (let [u1 (:id (make! User))
            u2 (:id (make! User))
            master (:id (make! Dashboard {:name "M" :owner-id u1}))]
        (is (submap? {:owner-id u2 :name "S" :master-id master :panels []}
                     (api/import {:user-id u2} {:master-id master :name "S"})))))

    (it "the master dashboard must exists"
      (let [u (:id (make! User))]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized"
                              (api/import {:user-id u} {:master-id u :name "S"}))))))

  (testing "find-all"
    (it "should return the user's dashboards only"
      (let [u1 (:id (make! User))
            u2 (:id (make! User))
            d1 (:id (make! Dashboard {:owner-id u1}))
            d2 (:id (make! Dashboard {:owner-id u2}))]
        (is (= [d1] (map :id (api/find-all {:user-id u1}))))
        (is (= [d2] (map :id (api/find-all {:user-id u2})))))))

  (testing "find-by-id"
    (it "should fetch the reports"
      (let [d (:id (make! Dashboard {:panels [{:report {:name "R1"}} {:report {:name "R2"}}]}))]
        (is (submaps? [{:name "R1"} {:name "R2"}]
                      (->> (api/find-by-id {} d) :panels (map :report))))))

    (it "if it's a slave, should fetch the master panels"
      (let [m (:id (make! Dashboard {:panels [{:report {:name "R"}}]}))
            s (:id (make! Dashboard {:master-id m}))]
        (is (submaps? [{:name "R"}]
                      (->> (api/find-by-id {} s) :panels (map :report))))))))
