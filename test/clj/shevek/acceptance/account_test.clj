(ns shevek.acceptance.account-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.keys :as k]))

(deftest account
  (it "the current password mast match"
    (login {:username "max" :fullname "Max" :password "secret999"})
    (click-link "Max")
    (is (has-text? "Your Account"))
    (click-link "Modify")
    (fill {:name "email"} "max@acme.com")
    (fill {:name "current-password"} "nop" k/enter)
    (is (has-css? ".red.label" :text "is incorrect")))

  (it "changing the email"
    (login {:username "max" :fullname "Max" :password "secret999"})
    (click-link "Max")
    (click-link "Modify")
    (fill {:name "email"} "max@acme.com")
    (fill {:name "current-password"} "secret999" k/enter)
    (is (has-css? "#notification" :text "Your account has been saved"))
    (is (has-css? ".segment .item" :text "max@acme.com"))))
