(ns shevek.acceptance.account-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.keys :as k]))

(deftest account
  (it "the current password mast match" page
    (login page {:username "max" :fullname "Max" :password "secret123"})
    (click-link page "Max")
    (is (has-text? page "Your Account"))
    (click-link page "Modify")
    (fill page {:name "email"} "max@acme.com")
    (fill page {:name "current-password"} "nop" k/enter)
    (is (has-css? page ".red.label" :text "is incorrect")))

  (it "changing the email" page
    (login page {:username "max" :fullname "Max" :password "secret123"})
    (click-link page "Max")
    (click-link page "Modify")
    (fill page {:name "email"} "max@acme.com")
    (fill page {:name "current-password"} "secret123" k/enter)
    (is (has-css? page "#notification" :text "Your account has been saved"))
    (is (has-css? page ".segment .item" :text "max@acme.com"))))
