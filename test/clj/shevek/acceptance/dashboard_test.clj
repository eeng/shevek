(ns shevek.acceptance.dashboard-test
  (:require [clojure.test :refer :all]
            [etaoin.api :refer :all]
            [shevek.test-helper :refer [spec]]))

(deftest dashboard
  (testing "muestra los cubos disponibles"
    (with-chrome {} driver
      (go driver "http://localhost:3200")
      (wait-has-text driver {:tag "h1"} "Dashboard"))))

#_(def driver (chrome))
#_(go driver "http://localhost:3200")
#_(wait-has-text driver {:tag "h1"} "Dashboard")
