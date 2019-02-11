(ns shevek.dashboards.repository-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.asserts :refer [submaps? without?]]
            [shevek.dashboards.repository :as r]
            [shevek.db :refer [db]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.lib.mongodb :as m]))

(use-fixtures :once wrap-unit-tests)

(deftest save-tests
  (it "should save each report in their collection and store only the ids in the dashboard"
    (r/save-dashboard db {:name "D" :panels [{:report {:name "R1"}} {:report {:name "R2"}}]})
    (let [reports (m/find-all db "reports")]
      (is (submaps? [{:name "R1"} {:name "R2"}]
                    reports))
      (is (submaps? [{:name "D" :panels [{:report-id (-> reports first :id)}
                                         {:report-id (-> reports second :id)}]}]
                    (m/find-all db "dashboards")))))

  (it "if the report is not present should not try to save it"
    (->> {:name "D" :panels [{:report {:name "R"}}]}
         (r/save-dashboard db)
         (r/save-dashboard db)) ; Here we have only the report-id
    (is (= 1 (m/count db "reports"))))

  ; So the dashboards reports don't appear independently in the reports page
  (it "should not set the owner-id of the reports"
    (r/save-dashboard db {:name "D" :panels [{:report {:name "R"}}]})
    (is (without? :owner-id (m/find-last db "reports")))))

(deftest find-by-id-tests
  (it "should fetch the reports"
    (let [d (r/save-dashboard db {:panels [{:report {:name "R1"}} {:report {:name "R2"}}]})]
      (is (submaps? [{:name "R1"} {:name "R2"}]
                    (->> (r/find-by-id db (:id d)) :panels (map :report)))))))

(deftest delete-dashboard-tests
  (it "should remove the dashboard and its reports"
    (let [d1 (r/save-dashboard db {:name "D1" :panels [{:report {:name "R1"}}]})]
      (r/save-dashboard db {:name "D2" :panels [{:report {:name "R2"}}]})
      (r/delete-dashboard db (:id d1))
      (is (submaps? [{:name "D2"}] (m/find-all db "dashboards")))
      (is (submaps? [{:name "R2"}] (m/find-all db "reports"))))))
