(ns shevek.dashboards.repository-test
  (:require [clojure.test :refer [deftest is]]
            [shevek.test-helper :refer [it]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.report :refer [Report]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.dashboards.repository :refer [find-by-id]]
            [shevek.db :refer [db]]))

(deftest find-by-id-tests
  (it "should fetch the reports"
    (let [d (make! Dashboard)
          r1 (make! Report {:name "R1" :dashboards-ids [(:id d)]})
          r2 (make! Report {:name "R2" :dashboards-ids [(:id d)]})]
      (is (= ["R1" "R2"]
             (->> (find-by-id db (:id d)) :reports
                  (map :name)))))))
