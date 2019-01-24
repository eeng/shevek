(ns shevek.dashboards.repository-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.asserts :refer [submaps?]]
            [shevek.dashboards.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.mongodb :as m]))

(use-fixtures :once wrap-unit-tests)

(deftest save-tests
  (it "should save each report in their respective collection and store only the ids in the dashboard"
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
    (is (= 1 (m/count db "reports")))))

(deftest find-by-id-tests
  (it "should fetch the reports"
    (let [d (r/save-dashboard db {:panels [{:report {:name "R1"}} {:report {:name "R2"}}]})]
      (is (submaps? [{:name "R1"} {:name "R2"}]
             (->> (r/find-by-id db (:id d)) :panels (map :report)))))))

; TODO DASHBOARD reactivar (aca se simplificaria si la relacion entre dashboard y report es one-to-many xq podriamos simplemente eliminar todos los reportes del dash)
; (deftest delete-dashboard-tests
;   (it "should remove the dashboard from the reports that contain it"
;     (let [d (make! Dashboard)
;           r (make! Report {:dashboards-ids [(:id d)]})]
;       (r/delete-dashboard db d)
;       (is (= [] (:dashboards-ids (m/find-by-id db "reports" (:id r))))))))
