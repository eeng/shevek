(ns shevek.reports.repository-test
  (:require [clojure.test :refer [deftest is]]
            [shevek.test-helper :refer [it]]
            [shevek.makers :refer [make!]]
            [shevek.asserts :refer [without? submaps?]]
            [shevek.schemas.report :refer [Report]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.db :refer [db]]))

(deftest save-report-tests
  (it "should add the report to the indicated dashboards"
    (let [d1 (:id (make! Dashboard))
          d2 (:id (make! Dashboard))]
      (make! Report {:dashboards-ids [d1 d2]}))))
