(ns shevek.acceptance.viewer-test
  (:require [clojure.test :refer :all]
            [shevek.acceptance.test-helper :refer :all]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.makers :refer [make!]]))

(deftest viewer
  (it "doesn't explode" page
    (make! Cube {:title "Sales"})
    (visit page "/")
    (is (has-title? page "Dashboard"))))
