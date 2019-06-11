(ns shevek.users.api-test
  (:require [clojure.test :refer [use-fixtures deftest is]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.asserts :refer [submaps?]]
            [shevek.users.api :as api]
            [shevek.makers :refer [make! make]]
            [shevek.schemas.user :refer [User]]
            [shevek.lib.mongodb :as m]
            [shevek.db :refer [db]]))

(use-fixtures :once wrap-unit-tests)

(deftest find-all-tests
  (it "only admins can list users"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized" (api/find-all {})))
    (is (= [] (api/find-all {:user {:admin true}})))))

(deftest save-tests
  (it "only admins can save users"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized"
                          (api/save {:user {}} (make User))))
    (is (= [] (m/find-all db "users")))))

(deftest delete-tests
  (it "only admins can delete users"
    (let [{:keys [id]} (make! User)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized"
                            (api/delete {:user {}} id)))
      (is (submaps? [{:id id}] (m/find-all db "users")))))

  (it "removes the user by id"
    (let [u1 (make! User)
          u2 (make! User)]
      (api/delete {:user {:admin true}} (:id u1))
      (is (submaps? [{:id (:id u2)}] (m/find-all db "users"))))))
