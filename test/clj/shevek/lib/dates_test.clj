(ns shevek.lib.dates-test
  (:require [clojure.test :refer :all]
            [shevek.lib.dates :refer [plus-period]]
            [clj-time.core :as t]))

(deftest plus-period-tests
  (is (= "2015-09-01T01:00:00.000Z"
         (plus-period "2015-09-01T00:00:00.000Z" "PT1H")))
  (is (= "2015-09-02T00:00:00.000Z"
         (plus-period "2015-09-01T00:00:00.000Z" "P1D")))
  (is (= "2015-02-08T00:00:00.000Z"
         (plus-period "2015-02-01T00:00:00.000Z" "P1W")))
  (is (= "2015-03-01T00:00:00.000Z"
         (plus-period "2015-02-01T00:00:00.000Z" "P1M"))))
