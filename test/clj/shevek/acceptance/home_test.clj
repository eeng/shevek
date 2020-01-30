(ns shevek.acceptance.home-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.acceptance.test-helper :refer [wrap-acceptance-tests has-css? it has-title? login]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.makers :refer [make!]]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance home-tests
  (it "shows the available cubes"
      (make! Cube {:title "Sales"})
      (make! Cube {:title "Inventory"})
      (login)
      (is (has-title? "Welcome"))
      (is (has-css? ".panel" :text "Inventory"))
      (is (has-css? ".panel" :text "Sales"))))
