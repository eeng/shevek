(ns shevek.lib.dates-test
  (:require [clojure.test :refer :all]
            [shevek.lib.dates :refer [plus-duration]]))

(deftest plus-duration-tests
  (is (= "2015-09-01T01:00Z"
         (plus-duration "2015-09-01T00:00:00.000Z" "PT1H")))
  (is (= "2015-09-02T00:00Z"
         (plus-duration "2015-09-01T00:00:00.000Z" "P1D"))))
