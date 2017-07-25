(ns shevek.lib.auth-test
  (:require [clojure.test :refer :all]
            [shevek.lib.auth :refer [authenticate-and-generate-token]]
            [shevek.test-helper :refer [it]]
            [shevek.config :refer [config]]
            [shevek.asserts :refer [submap? without?]]
            [shevek.makers :refer [make!]]
            [shevek.schemas.user :refer [User]]
            [shevek.db :refer [db]]
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

  (it "tokens are valid for one day"
    (make! User {:username "john" :password "secret123"})
    (let [{:keys [token]} (authenticate-and-generate-token db {:username "john" :password "secret123"})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Token is expired"
           (jwt/unsign token (config :jwt-secret) {:now (t/plus (t/now) (t/hours 25))}))))))
