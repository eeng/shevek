(ns shevek.reports.api-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.asserts :refer [submap? without?]]
            [shevek.reports.api :as api]
            [shevek.db :refer [db]]
            [shevek.lib.mongodb :as m]
            [shevek.makers :refer [make make!]]
            [shevek.schemas.user :refer [User]]
            [shevek.schemas.report :refer [Report]]
            [clj-time.core :as t]))

(use-fixtures :once wrap-unit-tests)

(deftest save-report-tests
  (it "should set the owner-id with the request's user"
    (let [id (:id (make! User))]
      (is (= id (:owner-id (api/save {:user-id id} (make Report)))))))

  (it "should not allow to save a report if the request's is not the owner"
    (let [id1 (:id (make! User))
          id2 (:id (make! User))]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized"
                            (api/save {:user-id id1} (make! Report {:owner-id id2})))))))

(deftest find-by-id-tests
  ; This way when the user tries to save a shared report, it would be treated as a new one
  (it "should remove the id and sharing fields if it's a shared report"
    (let [{:keys [id]} (make! Report {:sharing-digest "asdf"})
          r (api/find-by-id {} id)]
      (is (without? :id r))
      (is (without? :sharing-digest r)))))

(deftest share-url-tests
  (def req {:scheme "http" :headers {"host" "dw.com"}})

  (testing "when the report is new"
    (it "should save it and return the url with its id"
      (let [url (api/share-url req {})
            {:keys [id]} (m/find-last db "reports")]
        (is (some? id))
        (is (= (str "http://dw.com/reports/" id) url))
        (is (= 1 (m/count db "reports")))))

    (it "should store the user who is sharing it but not the owner"
      (api/share-url (assoc req :user-id "5c5c7f0a44d29c31647e8f7c") {:name "New Report" :owner-id "5c5c7f0a44d29c31647e8f7d"})
      (let [shared-report (m/find-last db "reports")]
        (is (submap? {:shared-by-id "5c5c7f0a44d29c31647e8f7c"
                      :name "New Report"}
                     shared-report))
        (is (without? :owner-id shared-report))))

    (it "should be an idempotent operation if the 'same' report is shared twice"
      (api/share-url req {:measures ["m1"]})
      (api/share-url req {:measures ["m1"]})
      (is (= 1 (m/count db "reports")))
      (api/share-url req {:measures ["m1" "m2"]})
      (is (= 2 (m/count db "reports"))))

    (it "should set the timestamps correctly"
      (api/share-url req {:measures ["m1"]})
      (Thread/sleep 1)
      (api/share-url req {:measures ["m1"]})
      (let [{:keys [created-at updated-at]} (m/find-last db "reports")]
        (is (some? created-at))
        (is (some? updated-at))
        (is (t/after? updated-at created-at))))

    (it "should create a new shared report if a shared report is shared again"
      (api/share-url req {:measures ["m1"]})
      (let [shared-report (m/find-last db "reports")]
        (api/share-url req (assoc shared-report :measures ["m2"]))
        (is (= 2 (m/count db "reports")))))

    (it "when the report exists should still create a new one to be shared as we want a point-in-time snapshot"
      (let [url (api/share-url req {:id "123"})
            {:keys [id]} (m/find-last db "reports")]
        (is (some? id))
        (is (= (str "http://dw.com/reports/" id) url))
        (is (= 1 (m/count db "reports")))))))
