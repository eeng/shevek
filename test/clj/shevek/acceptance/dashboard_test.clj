(ns shevek.acceptance.dashboard-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :refer :all]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.makers :refer [make!]]))

(deftest dashboard
  (it "shows the available cubes" page
    (make! Cube {:title "Sales"})
    (make! Cube {:title "Inventory"})
    (visit page "/")
    (is (has-title? page "Dashboard"))
    (is (has-css? page ".cube.card:nth-child(1)" :text "Sales"))
    (is (has-css? page ".cube.card:nth-child(2)" :text "Inventory")))

  (it "the cubes are displayed also on the menu" page
    (make! Cube {:title "Sales"})
    (make! Cube {:title "Inventory"})
    (visit page "/")
    (click page {:css "#cubes-menu"})
    (is (has-css? page "#cubes-popup .item" :count 2))))