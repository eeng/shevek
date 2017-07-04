(ns shevek.acceptance.dashboard-test
  (:require [clojure.test :refer :all]
            [etaoin.api :refer :all]))

(deftest ^:acceptance test-xxx
  (with-chrome {} driver
    (go driver "http://localhost:3110")
    (Thread/sleep 1000)))
    ; (wait-has-text driver {:tag "h1"} "Dashboard")))

#_(def driver (chrome))
#_(go driver "http://localhost:3110")
#_(wait-has-text driver {:tag "h1"} "Dashboard")
