(ns shevek.engine.druid.planner.timeseries-test
  (:require [clojure.test :refer :all]
            [shevek.asserts :refer [submap? without? submaps?]]
            [shevek.engine.druid.planner.timeseries :refer [to-druid-query from-druid-results]]))

(deftest to-druid-query-test
  (testing "general structure"
    (is (submap? {:queryType "timeseries"
                  :dataSource "wikiticker"
                  :granularity "all"
                  :intervals "2015/2016"
                  :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}]}
                 (to-druid-query {:cube "wikiticker"
                                  :measures [{:name "count" :expression "(sum $count)"}]
                                  :filters [{:interval ["2015" "2016"]}]}))))

  (testing "granularity, sort order and timezone (default to local)"
    (is (submap? {:queryType "timeseries"
                  :intervals "2015/2016"
                  :granularity {:type "period" :period "P1D" :timeZone "America/Argentina/Buenos_Aires"}
                  :descending true}
                 (to-druid-query {:filters [{:interval ["2015" "2016"]}]
                                  :dimension {:name "__time" :granularity "P1D" :sort-by {:descending true}}})))
    (is (submap? {:granularity {:type "period" :period "P1D" :timeZone "Europe/Paris"}}
                 (to-druid-query {:dimension {:name "__time" :granularity "P1D"}
                                  :time-zone "Europe/Paris"}))))

  (testing "extraction functions"
    (testing "should use the dimension extraction function in filters"
      (is (= {:type "selector" :dimension "__time" :value "2017"
              :extractionFn {:type "strlen"}}
             (:filter
              (to-druid-query {:filters [{:name "year" :operator "is" :value "2017" :column "__time"
                                          :extraction [{:type "strlen"}]}]})))))))

(deftest from-druid-results-test
  (testing "normalize the results"
    (is (submaps? [{:__time "2015" :count 1}
                   {:__time "2016" :count 2}]
                  (from-druid-results
                   {:dimension {:name "__time" :limit 2}}
                   [{:result {:count 1} :timestamp "2015"}
                    {:result {:count 2} :timestamp "2016"}
                    {:result {:count 3} :timestamp "2017"}])))))
