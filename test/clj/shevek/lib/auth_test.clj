(ns shevek.lib.auth-test
  (:require [clojure.test :refer [use-fixtures deftest is are]]
            [shevek.test-helper :refer [it wrap-unit-tests]]
            [shevek.lib.auth :as auth]
            [shevek.asserts :refer [submap? without?]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.user :refer [User]]
            [shevek.db :refer [db]]
            [shevek.users.repository :refer [reload]]
            [clj-time.core :as t]))

(use-fixtures :once wrap-unit-tests)

(deftest login-tests
  (it "valid credentials should return a 201 with the user data"
    (make! User {:username "john" :password "secret123"})
    (let [{:keys [status body]} (auth/login {:params {:username "john" :password "secret123"}})]
      (is (= 201 status))
      (is (submap? {:username "john"} (:user body)))
      (is (without? :password (:user body)))))

  (it "non-existent user should return error"
    (let [{:keys [status body]} (auth/login {:params {:username "john"}})]
      (is (= 401 status))
      (is (= :invalid-credentials (:error body)))))

  (it "invalid credentials user should return error"
    (make! User {:username "john" :password "secret123"})
    (are [expected-error credentials] (= expected-error (get-in (auth/login {:params credentials}) [:body :error]))
      :invalid-credentials {:username "john" :password "secret123 "}
      :invalid-credentials {:username "john" :password "Secret123"}
      :invalid-credentials {:username "John" :password "secret123"}
      :invalid-credentials {:username "" :password "secret123"}
      :invalid-credentials {:username "john" :password ""}
      nil {:username "john" :password "secret123"}))

  (it "on successful login should update previous-sign-in-at with last-sign-in-at and last-sign-in-at field with now"
    (let [{:keys [updated-at] :as u} (make! User {:username "john" :password "secret123"})
          t1 (t/date-time 2017)
          t2 (t/date-time 2018)]
      (with-redefs [t/now (constantly t1)]
        (is (= 201 (:status (auth/login {:params {:username "john" :password "secret123"}}))))
        (is (submap? {:previous-sign-in-at nil
                      :last-sign-in-at (t/to-time-zone t1 (t/default-time-zone))
                      :updated-at (t/to-time-zone updated-at (t/default-time-zone))}
                     (reload db u))))
      (with-redefs [t/now (constantly t2)]
        (is (= 201 (:status (auth/login {:params {:username "john" :password "secret123"}}))))
        (is (submap? {:previous-sign-in-at (t/to-time-zone t1 (t/default-time-zone))
                      :last-sign-in-at (t/to-time-zone t2 (t/default-time-zone))
                      :updated-at (t/to-time-zone updated-at (t/default-time-zone))}
                     (reload db u)))))))
