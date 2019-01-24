(ns shevek.acceptance.home-test
  (:require [clojure.test :refer [deftest use-fixtures is]]
            [shevek.acceptance.test-helper :refer [wrap-acceptance-tests click click-link has-css? it has-title? login]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.makers :refer [make!]]))

(use-fixtures :once wrap-acceptance-tests)

(deftest ^:acceptance home
  (it "shows the available cubes"
    (make! Cube {:title "Sales"})
    (make! Cube {:title "Inventory"})
    (login)
    (is (has-title? "Welcome"))
    (is (has-css? ".cube.card:nth-child(1)" :text "Inventory"))
    (is (has-css? ".cube.card:nth-child(2)" :text "Sales")))

  (it "the cubes are displayed also on the menu"
    (make! Cube {:title "Sales"})
    (make! Cube {:title "Inventory"})
    (login)
    (click {:css "#cubes-menu"})
    (is (has-css? "#cubes-popup .item" :count 2))))
