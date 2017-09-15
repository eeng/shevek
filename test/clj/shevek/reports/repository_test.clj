(ns shevek.reports.repository-test
  (:require [clojure.test :refer [deftest is]]
            [shevek.test-helper :refer [it]]
            [shevek.makers :refer [make!]]
            [shevek.asserts :refer [submap? without?]]
            [shevek.schemas.report :refer [Report]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.dashboards.repository :as dr]
            [shevek.reports.repository :refer [save-report]]
            [shevek.db :refer [db]]
            [shevek.lib.mongodb :refer [oid]]))

(deftest save-report-tests
  (it "should add the report to the indicated dashboards"
    (let [d1 (make! Dashboard)
          r1 (make! Report)
          d2 (make! Dashboard {:reports [{:report-id (:id r1)}]})
          r2 (make! Report {:dashboards-ids [(:id d1) (:id d2)]})
          new-d1 (dr/find-by-id db (:id d1))
          new-d2 (dr/find-by-id db (:id d2))]
      (is (submap? {:reports [{:report-id (:id r2)}]} new-d1))
      (is (= (dissoc d1 :created-at :updated-at)
             (dissoc new-d1 :reports :created-at :updated-at)))
      (is (submap? {:reports [{:report-id (:id r1)} {:report-id (:id r2)}]} new-d2))))

  (it "should not add it if the dashboard already contains it"
    (let [d1 (make! Dashboard)
          r1 (make! Report {:dashboards-ids [(:id d1)]})]
      (save-report db r1)
      (is (submap? {:reports [{:report-id (:id r1)}]}
                   (dr/find-by-id db (:id d1))))))

  (it "should remove the report from not selected dashboards"
    (let [d1 (make! Dashboard)
          d2 (make! Dashboard)
          r1 (make! Report {:dashboards-ids [(:id d1)]})]
      (save-report db (assoc r1 :dashboards-ids [(:id d2)]))
      (is (submap? {:reports []} (dr/find-by-id db (:id d1))))
      (is (submap? {:reports [{:report-id (:id r1)}]} (dr/find-by-id db (:id d2))))))

  (it "should not touch other dashboards"
    (let [d1 (make! Dashboard)
          d2 (make! Dashboard)
          r1 (make! Report {:dashboards-ids [(:id d1)]})]
      (is (without? :reports (dr/find-by-id db (:id d2)))))))
