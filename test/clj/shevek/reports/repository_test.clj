(ns shevek.reports.repository-test
  (:require [clojure.test :refer [use-fixtures deftest testing is]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.makers :refer [make make!]]
            [shevek.schemas.report :refer [Report]]
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
