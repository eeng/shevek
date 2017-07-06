(ns shevek.lib.auth-test
  (:require [clojure.test :refer :all]
            [shevek.lib.auth :refer [authenticate]]
            [shevek.test-helper :refer [it]]
            [shevek.config :refer [config]]
            [shevek.asserts :refer [submap?]]
            [shevek.makers :refer [make!]]
            [shevek.users.repository :refer [User]]
            [shevek.db :refer [db]]
            [buddy.sign.jwt :as jwt]))

(deftest authenticate-tests
  (it "valid credentials should return a JSON Web Token"
    (make! User {:username "john" :password "secret123"})
    (let [{:keys [status token]} (authenticate db {:username "john" :password "secret123"})]
      (is (= :success status))
      (is (submap? {:username "john"}  (jwt/unsign token (config :jwt-secret))))))

  (it "non-existent user should return an error message"
    (let [{:keys [status error]} (authenticate db {:username "john"})]
      (is (= :failure status))
      (is (= :invalid-credentials error))))

  (it "invalid credentials user should return an error message"
    (make! User {:username "john" :password "secret123"})
    (are [expected-error credentials] (= expected-error (:error (authenticate db credentials)))
      :invalid-credentials {:username "john" :password "secret123 "}
      :invalid-credentials {:username "john" :password "Secret123"}
      :invalid-credentials {:username "John" :password "secret123"}
      :invalid-credentials {:username "" :password "secret123"}
      :invalid-credentials {:username "john" :password ""}
      nil {:username "john" :password "secret123"})))
