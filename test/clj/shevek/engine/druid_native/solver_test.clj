(ns shevek.engine.druid-native.solver-test
  (:require [clojure.test :refer [deftest testing is]]
            [shevek.asserts :refer [submaps?]]
            [shevek.engine.druid-native.solver :refer [translate-lookups]]))

(deftest translate-lookups-test
  (testing "should resolve the dimension lookups"
    (is (submaps? [{:month "January"} {:month nil} {:month "May"}]
                  (translate-lookups
                   [{:month 1} {:month 2} {:month 5}]
                   {:dimension
                    {:name "month"
                     :lookup [1 "January" 5 "May"]}})))))
