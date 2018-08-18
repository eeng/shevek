(ns shevek.acceptance.auth-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.keys :as k]
            [shevek.schemas.user :refer [User]]
            [shevek.makers :refer [make!]]
            [shevek.lib.auth :refer [token-expiration]]
            [clj-time.core :as t]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance authentication
  (it "invalid credentials"
    (make! User {:username "max" :password "payne"})
    (visit "/")
    (click-link "Login")
    (is (has-text? "Invalid username"))
    (fill {:name "username"} "max" k/enter)
    (is (has-text? "Invalid username"))
    (fill {:name "password"} "nop" k/enter)
    (is (has-text? "Invalid username")))

  (it "logout"
    (login)
    (click-link "Logout")
    (is (has-css? "#login"))
    (is (has-no-text? "Logout")))

  (it "session expired"
    (with-redefs [token-expiration (t/seconds 1)]
      (login-admin))
    (Thread/sleep 1100)
    (when (has-text? "Dashboard")
      (click {:css "i.users"}))
    (is (has-text? "Session expired"))
    (is (has-css? "#login"))))
