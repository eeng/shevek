(ns shevek.acceptance.auth-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :as e :refer [refresh]]
            [etaoin.keys :as k]
            [shevek.users.repository :refer [User]]
            [shevek.makers :refer [make!]]
            [shevek.lib.auth :refer [token-expiration]]
            [clj-time.core :as t]))

(deftest authentication
  (it "invalid credentials" page
    (make! User {:username "max" :password "payne"})
    (visit page "/")
    (click-link page "Login")
    (is (has-text? page "Invalid username"))
    (fill page {:name "username"} "max" k/enter)
    (is (has-text? page "Invalid username"))
    (fill page {:name "password"} "nop" k/enter)
    (is (has-text? page "Invalid username")))

  (it "logout" page
    (login page)
    (click-link page "Logout")
    (is (has-css? page "#login"))
    (is (has-no-text? page "Logout")))

  (it "session expired" page
    (with-redefs [token-expiration (t/seconds 1)]
      (login page))
    (Thread/sleep 1100)
    (when (e/has-text? page "Manage")
      (click-link page "Manage"))
    (is (has-text? page "Session expired"))
    (is (has-css? page "#login"))))
