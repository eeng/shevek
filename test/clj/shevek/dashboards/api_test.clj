(ns shevek.dashboards.api-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.dashboards.api :as api]
            [shevek.dashboards.repository :as r]
            [shevek.db :refer [db]]
            [shevek.makers :refer [make make!]]
            [shevek.asserts :refer [submap? submaps? without?]]
            [shevek.schemas.user :refer [User]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.lib.mongodb :as m]))

(use-fixtures :once wrap-unit-tests)

(deftest save-tests
  (it "create should set the owner"
    (let [u1 (:id (make! User))]
      (is (= u1 (:owner-id (api/save {:user-id u1} (make Dashboard)))))))

  (it "update can only be made by the owner"
    (let [u1 (:id (make! User))
          u2 (:id (make! User))
          d (make! Dashboard {:owner-id u1})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized"
                            (api/save {:user-id u2} d))))))

(deftest find-all-tests
  (it "should return the user's dashboards only"
    (let [u1 (:id (make! User))
          u2 (:id (make! User))
          d1 (:id (make! Dashboard {:owner-id u1}))
          d2 (:id (make! Dashboard {:owner-id u2}))]
      (is (= [d1] (map :id (api/find-all {:user-id u1}))))
      (is (= [d2] (map :id (api/find-all {:user-id u2})))))))

(deftest find-by-id-tests
  (it "should fetch the reports"
    (let [d (:id (make! Dashboard {:panels [{:report {:name "R1"}} {:report {:name "R2"}}]}))]
      (is (submaps? [{:name "R1"} {:name "R2"}]
                    (->> (api/find-by-id {} d) :panels (map :report))))))

  (it "if it's a slave, should fetch the master panels"
    (let [m (:id (make! Dashboard {:panels [{:report {:name "R"}}]}))
          s (:id (make! Dashboard {:master-id m}))]
      (is (submaps? [{:name "R"}]
                    (->> (api/find-by-id {} s) :panels (map :report)))))))

(deftest import-tests
  (it "as a link (slave) should set the master-id and no panels",
    (let [u1 (:id (make! User))
          u2 (:id (make! User))
          orig (:id (make! Dashboard {:name "M" :owner-id u1 :panels [{:report {:name "R"}}]}))
          link (->> (api/import {:user-id u2} {:import-as "link" :original-id orig :name "S"})
                    :id (r/find-by-id db))]
      (is (submap? {:owner-id u2 :name "S" :master-id orig :panels []} link))))

  (it "as a copy (master) should duplicate the panels",
    (let [u1 (:id (make! User))
          u2 (:id (make! User))
          orig (make! Dashboard {:name "M1" :owner-id u1 :panels [{:report {:name "R"}}]})
          copy (->> (api/import {:user-id u2} {:import-as "copy" :original-id (:id orig) :name "M2"})
                    :id (r/find-by-id db))
          [r1 r2 :as reports] (m/find-all db "reports")]
      (is (= 2 (count reports)))
      (is (submaps? [{:report-id (:id r1)}] (:panels orig)))
      (is (submaps? [{:report-id (:id r2)}] (:panels copy)))
      (is (without? :master-id copy))))

  (it "the master dashboard must exists"
    (let [u (:id (make! User))]
      (is (thrown? java.lang.AssertionError
                   (api/import {:user-id u} {:import-as "link" :original-id u :name "S"}))))))
