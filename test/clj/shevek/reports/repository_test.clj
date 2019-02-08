(ns shevek.reports.repository-test
  (:require [clojure.test :refer [use-fixtures deftest testing is]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.makers :refer [make make!]]
            [shevek.asserts :refer [submap? without?]]
            [shevek.schemas.report :refer [Report]]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.dashboards.repository :as dr]
            [shevek.reports.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.mongodb :as m :refer [oid]]
            [clj-time.core :as t]))

(use-fixtures :once wrap-unit-tests)

(deftest delete-old-shared-reports-tests
  (it "should delete only shared-reports"
    (let [id1 (:id (make! Report))]
      (make! Report {:sharing-digest "..."})
      (r/delete-old-shared-reports db {:older-than (-> (t/seconds 1) t/from-now)})
      (is (= [id1] (map :id (m/find-all db "reports"))))))

  (it "by default should delete only reports older than 30 days"
    (let [id1 (:id (make! Report {:sharing-digest "..."}))
          id2 (:id (make! Report {:sharing-digest "..."}))]
      (m/update-by-id db "reports" id1 {:updated-at (-> (t/days 31) t/ago)})
      (m/update-by-id db "reports" id2 {:updated-at (-> (t/days 29) t/ago)})
      (r/delete-old-shared-reports db)
      (is (= [id2] (map :id (m/find-all db "reports")))))))

; TODO DASHBOARD reactivar
; (defn reload-dashboard [{:keys [id]}]
;   (m/find-by-id db "dashboards" id))

; (deftest save-report-tests
;   (it "should add the report to the indicated dashboards"
;     (let [d1 (make! Dashboard)
;           r1 (make! Report)
;           d2 (make! Dashboard {:panels [{:report-id (:id r1)}]})
;           r2 (make! Report {:dashboards-ids [(:id d1) (:id d2)]})
;           new-d1 (reload-dashboard d1)
;           new-d2 (reload-dashboard d2)]
;       (is (submap? {:panels [{:report-id (:id r2)}]} new-d1))
;       (is (= (dissoc d1 :created-at :updated-at)
;              (dissoc new-d1 :panels :created-at :updated-at)))
;       (is (submap? {:panels [{:report-id (:id r1)} {:report-id (:id r2)}]} new-d2))))
;
;   (it "should not add it if the dashboard already contains it"
;     (let [d1 (make! Dashboard)
;           r1 (make! Report {:dashboards-ids [(:id d1)]})]
;       (save-report db r1)
;       (is (submap? {:panels [{:report-id (:id r1)}]}
;                    (reload-dashboard d1)))))
;
;   (it "should remove the report from not selected dashboards"
;     (let [d1 (make! Dashboard)
;           d2 (make! Dashboard)
;           r1 (make! Report {:dashboards-ids [(:id d1)]})]
;       (save-report db (assoc r1 :dashboards-ids [(:id d2)]))
;       (is (submap? {:panels []} (reload-dashboard d1)))
;       (is (submap? {:panels [{:report-id (:id r1)}]} (reload-dashboard d2)))))
;
;   (it "should not touch other dashboards"
;     (let [d1 (make! Dashboard)
;           d2 (make! Dashboard)
;           r1 (make! Report {:dashboards-ids [(:id d1)]})]
;       (is (without? :panels (reload-dashboard d2))))))

  ; (deftest delete-report-tests
  ;   (it "should remove the report from the dashboards that contain it"
  ;     (let [d (make! Dashboard)
  ;           r (make! Report {:dashboards-ids [(:id d)]})]
  ;       (delete-report db r)
  ;       (is (= [] (:panels (reload-dashboard d)))))))
