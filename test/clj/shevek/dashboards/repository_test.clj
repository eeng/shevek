(ns shevek.dashboards.repository-test
  (:require [clojure.test :refer [deftest is]]
            [shevek.test-helper :refer [it]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.report :refer [Report]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.dashboards.repository :refer [find-by-id delete-dashboard]]
            [shevek.db :refer [db]]
            [shevek.lib.mongodb :as m]))

(deftest find-by-id-tests
  (it "should fetch the reports"
    (let [d (make! Dashboard)
          r1 (make! Report {:name "R1" :dashboards-ids [(:id d)]})
          r2 (make! Report {:name "R2" :dashboards-ids [(:id d)]})]
      (is (= ["R1" "R2"]
             (->> (find-by-id db (:id d)) :reports
                  (map :name)))))))

(deftest delete-dashboard-tests
  (it "should remove the dashboard from the reports that contain it"
    (let [d (make! Dashboard)
          r (make! Report {:dashboards-ids [(:id d)]})]
      (delete-dashboard db d)
      (is (= [] (:dashboards-ids (m/find-by-id db "reports" (:id r))))))))
