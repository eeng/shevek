(ns shevek.engine.druid.planner.groupby-test
  (:require [clojure.test :refer :all]
            [shevek.asserts :refer [submap? without? submaps?]]
            [shevek.engine.druid.planner.groupby :refer [to-druid-query from-druid-results]]))

(deftest to-druid-query-test
  (testing "general structure"
    (let [dq (to-druid-query
              {:dimension {:name "monthName"
                           :sort-by {:name "monthNum" :descending false :type "LONG"}}})]
      (is (submap? {:queryType "groupBy"
                    :dimensions ["monthNum", "monthName"]
                    :granularity "all"}
                   dq))
      (is (without? :metric dq))
      (is (without? :dimension dq))))

  (testing "limit should be added through a limitSpec"
    (let [dq (to-druid-query
              {:dimension {:sort-by {:name "monthNum" :descending false :type "LONG"}
                           :limit 5}})]
      (is (submap? {:type "default" :limit 5}
                   (:limitSpec dq)))
      (is (without? :threshold dq))))

  (testing "sorting"
    (is (submap? {:queryType "groupBy"
                  :limitSpec {:type "default"
                              :limit 100
                              :columns [{:dimension "monthNum"
                                         :direction "descending"
                                         :dimensionOrder "numeric"}]}}
                 (to-druid-query
                  {:dimension {:name "monthName"
                               :sort-by {:name "monthNum" :descending true :type "LONG"}}}))))

  (testing "dimension expressions"
    (let [dq (to-druid-query
              {:dimension {:name "monthName"
                           :sort-by {:name "monthNum" :descending true :type "LONG"
                                     :expression "timestamp_extract(__time, 'MONTH')"}}})]
      (is (= [{:type "expression"
               :expression "timestamp_extract(__time, 'MONTH')"
               :outputType "LONG"
               :name "monthNum:v"}]
             (:virtualColumns dq)))
      (is (submap? {:dimension "monthNum:v"}
                   (first (:dimensions dq)))))))

(deftest from-druid-results-test
  (testing "results normalization"
    (is (submaps?
         [{:countryName "Albania" :countryIsoCode "AL" :count 2}
          {:countryName "Angola" :countryIsoCode "AO" :count 4}]
         (from-druid-results
          [{:version "v1"
            :timestamp "2015-09-12T02:00:00.000Z"
            :event
            {:count 2
             :countryName "Albania"
             :countryIsoCode "AL"}}
           {:version "v1"
            :timestamp "2015-09-12T02:00:00.000Z"
            :event
            {:count 4
             :countryName "Angola"
             :countryIsoCode "AO"}}])))))
