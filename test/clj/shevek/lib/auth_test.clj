(ns shevek.lib.auth-test
  (:require [clojure.test :refer :all]
            [shevek.lib.auth :refer [authenticate-and-generate-token]]
            [shevek.test-helper :refer [it]]
            [shevek.config :refer [config]]
            [shevek.asserts :refer [submap? without?]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.user :refer [User]]
            [shevek.db :refer [db]]
            [shevek.users.repository :refer [reload]]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as t]))

(deftest authenticate-and-generate-token-tests
  (it "valid credentials should return a JSON Web Token with the user data (except password)"
    (make! User {:username "john" :password "secret123"})
    (let [{:keys [token]} (authenticate-and-generate-token db {:username "john" :password "secret123"})
          user (jwt/unsign token (config :jwt-secret))]
      (is (submap? {:username "john"} user))
      (is (without? :password user))))

  (it "non-existent user should return error"
    (let [{:keys [error]} (authenticate-and-generate-token db {:username "john"})]
      (is (= :invalid-credentials error))))

  (it "invalid credentials user should return error"
    (make! User {:username "john" :password "secret123"})
    (are [expected-error credentials] (= expected-error (:error (authenticate-and-generate-token db credentials)))
      :invalid-credentials {:username "john" :password "secret123 "}
      :invalid-credentials {:username "john" :password "Secret123"}
      :invalid-credentials {:username "John" :password "secret123"}
      :invalid-credentials {:username "" :password "secret123"}
      :invalid-credentials {:username "john" :password ""}
      nil {:username "john" :password "secret123"}))

  (it "tokens are valid for one week"
    (make! User {:username "john" :password "secret123"})
    (let [{:keys [token]} (authenticate-and-generate-token db {:username "john" :password "secret123"})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Token is expired"
           (jwt/unsign token (config :jwt-secret) {:now (t/plus (t/now) (t/days 8))})))))

  (it "on successful login should update previous-sign-in-at with last-sign-in-at and last-sign-in-at field with now"
    (let [{:keys [updated-at] :as u} (make! User {:username "john" :password "secret123"})
          t1 (t/date-time 2017)
          t2 (t/date-time 2018)]
      (with-redefs [t/now (constantly t1)]
        (is (contains? (authenticate-and-generate-token db {:username "john" :password "secret123"}) :token))
        (is (submap? {:previous-sign-in-at nil
                      :last-sign-in-at (t/to-time-zone t1 (t/default-time-zone))
                      :updated-at (t/to-time-zone updated-at (t/default-time-zone))}
                     (reload db u))))
      (with-redefs [t/now (constantly t2)]
        (is (contains? (authenticate-and-generate-token db {:username "john" :password "secret123"}) :token))
        (is (submap? {:previous-sign-in-at (t/to-time-zone t1 (t/default-time-zone))
                      :last-sign-in-at (t/to-time-zone t2 (t/default-time-zone))
                      :updated-at (t/to-time-zone updated-at (t/default-time-zone))}
                     (reload db u)))))))
