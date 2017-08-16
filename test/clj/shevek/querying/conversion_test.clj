(ns shevek.querying.conversion-test
  (:require [clojure.test :refer :all]
            [shevek.asserts :refer [submap? without? submaps?]]
            [shevek.querying.conversion :refer [to-druid-query from-druid-results]]))

(deftest to-druid-query-test
  (testing "totals query"
    (is (submap? {:queryType "timeseries"
                  :dataSource {:type "table" :name "wikiticker"}
                  :granularity "all"
                  :intervals "2015/2016"
                  :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}]}
                 (to-druid-query {:cube "wikiticker"
                                  :measures [{:name "count" :expression "(sum $count)"}]
                                  :filter [{:interval ["2015" "2016"]}]}))))

  (testing "query with one atemporal dimension should generate a topN query"
    (is (submap? {:queryType "topN"
                  :dataSource {:type "table" :name "wikiticker"}
                  :granularity "all"
                  :dimension "page"
                  :metric {:type "numeric" :metric "count"}
                  :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}]
                  :threshold 10}
                 (to-druid-query {:cube "wikiticker"
                                  :dimension {:name "page" :limit 10}
                                  :measures [{:name "count" :expression "(sum $count)"}]}))))

  (testing "query with one time dimension should generate a timeseries query"
    (is (submap? {:queryType "timeseries"
                  :dataSource {:type "table" :name "wikiticker"}
                  :intervals "2015/2016"
                  :granularity {:type "period"
                                :period "P1D"}
                  :descending true}
                 (to-druid-query {:cube "wikiticker"
                                  :filter [{:interval ["2015" "2016"]}]
                                  :dimension {:name "__time" :granularity "P1D" :sort-by {:descending true}}}))))

  (testing "filter"
    (testing ":is operator"
      (is (submap? {:filter {:type "selector"
                             :dimension "isRobot"
                             :value "true"}}
                   (to-druid-query {:filter [{:name "isRobot" :operator "is" :value "true"}]})))
      (is (submap? {:filter {:type "selector"
                             :dimension "countryName"
                             :value nil}}
                   (to-druid-query {:filter [{:name "countryName" :operator "is" :value nil}]}))))

    (testing "two filters should generate an 'and' filter"
      (is (submap? {:filter {:type "and"
                             :fields [{:type "selector" :dimension "isRobot" :value "true"}
                                      {:type "selector" :dimension "isNew" :value "false"}]}}
                   (to-druid-query {:filter [{:name "__time"} ; Este se ignora xq se manda en el interval
                                             {:name "isRobot" :operator "is" :value "true"}
                                             {:name "isNew" :operator "is" :value "false"}]})))
      (is (without? :filter (to-druid-query {:filter [{:name "isRobot"} {:name "isNew"}]})))
      (is (without? :filter (to-druid-query {:filter [{:name "isRobot" :operator "include" :value []}]}))))

    (testing ":include operator"
      (is (submap? {:filter {:type "in"
                             :dimension "countryName"
                             :values ["Italy" "France"]}}
                   (to-druid-query {:filter [{:name "countryName" :operator "include" :value ["Italy" "France"]}]}))))

    (testing ":exclude operator"
      (is (submap? {:filter {:type "not"
                             :field {:type "in"
                                     :dimension "countryName"
                                     :values [nil "France"]}}}
                   (to-druid-query {:filter [{:name "countryName" :operator "exclude" :value [nil "France"]}]}))))

    (testing ":search operator"
      (is (submap? {:filter {:type "search"
                             :dimension "countryName"
                             :query {:type "insensitive_contains"
                                     :value "arg"}}}
                   (to-druid-query {:filter [{:name "countryName" :operator "search" :value "arg"}]})))))

  (testing "sorting"
    (testing "query with one atemporal dimension sorting by other selected metric in ascending order"
      (is (submap? {:queryType "topN"
                    :metric {:type "inverted" :metric {:type "numeric" :metric "added"}}}
                   (to-druid-query {:cube "wikiticker"
                                    :dimension {:name "page" :sort-by {:name "added" :descending false}}
                                    :measures [{:name "count" :expression "(sum $count)"}
                                               {:name "added" :expression "(sum $added)"}]}))))

    (testing "query with one atemporal dimension sorting by other non selected metric should add it to the aggregations"
      (is (submap? {:metric {:type "numeric" :metric "users"}
                    :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}
                                   {:name "users" :fieldName "users" :type "hyperUnique"}]}
                   (to-druid-query {:dimension {:name "page"
                                                :sort-by {:name "users" :expression "(count-distinct $users)"}}
                                    :measures [{:name "count" :expression "(sum $count)"}]}))))

    (testing "ascending ordered by the same dimension should use lexicographic sorting"
      (is (submap? {:metric {:type "dimension" :ordering "lexicographic"}}
                   (to-druid-query {:dimension {:name "page" :sort-by {:name "page" :descending false}}
                                    :measures [{:name "count" :expression "(sum $count)"}]}))))

    (testing "descending ordered by the same dimension should use lexicographic sorting"
      (is (submap? {:metric {:type "inverted"
                             :metric {:type "dimension" :ordering "lexicographic"}}}
                   (to-druid-query {:dimension {:name "page" :sort-by {:name "page" :descending true}}})))))

  (testing "measures"
    (testing "arithmetic expression"
      (is (submap? {:aggregations [{:fieldName "amount" :name "_t0" :type "doubleSum"}]
                    :postAggregations [{:type "arithmetic"
                                        :name "amount"
                                        :fn "/"
                                        :fields [{:type "fieldAccess" :fieldName "_t0"}
                                                 {:type "constant" :value 100}]}]}
                   (to-druid-query {:measures [{:name "amount" :expression "(/ (sum $amount) 100)"}]}))))

    (testing "two expressions referring to the same measure"
      (is (submap? {:aggregations [{:fieldName "amount" :name "_t0" :type "doubleSum"}
                                   {:fieldName "amount" :name "_t1" :type "doubleSum"}]
                    :postAggregations [{:type "arithmetic" :name "a1" :fn "/"
                                        :fields [{:type "fieldAccess" :fieldName "_t0"}
                                                 {:type "constant" :value 10}]}
                                       {:type "arithmetic" :name "a2" :fn "*"
                                        :fields [{:type "fieldAccess" :fieldName "_t1"}
                                                 {:type "constant" :value 20}]}]}
                   (to-druid-query {:measures [{:name "a1" :expression "(/ (sum $amount) 10)"}
                                               {:name "a2" :expression "(* (sum $amount) 20)"}]})))))

  (testing "extraction functions"
    (testing "derived dimension with one extraction fn"
      (is (= {:type "extraction" :outputName "year" :dimension "__time"
              :extractionFn {:type "timeFormat" :format "Y"}}
             (:dimension
               (to-druid-query {:dimension {:name "year" :column "__time"
                                            :extraction [{:type "timeFormat" :format "Y"}]}})))))

    (testing "derived dimension with two extraction fns"
      (is (= {:type "extraction" :outputName "year" :dimension "__time"
              :extractionFn {:type "cascade" :extractionFns [{:type "timeFormat" :format "Y"}
                                                             {:type "strlen"}]}}
             (:dimension
               (to-druid-query {:dimension {:name "year" :column "__time"
                                            :extraction [{:type "timeFormat" :format "Y"}
                                                         {:type "strlen"}]}}))))))

  (testing "timeout"
    (is (= 30000 (get-in (to-druid-query {}) [:context :timeout])))))

(deftest from-druid-results-test
  (testing "topN results"
    (is (submaps? [{:page "P1" :count 1}
                   {:page "P2" :count 2}]
                  (from-druid-results
                    {:dimension {:name "page"}}
                    {:queryType "topN"}
                    [{:result [{:page "P1" :count 1}
                               {:page "P2" :count 2}]}]))))

  (testing "timeseries results"
    (is (submaps? [{:__time "2015" :count 1}
                   {:__time "2016" :count 2}
                   {:__time "2017" :count 3}]
                  (from-druid-results
                   {:dimension {:name "__time"}}
                   {:queryType "timeseries"}
                   [{:result {:count 1} :timestamp "2015"}
                    {:result {:count 2} :timestamp "2016"}
                    {:result {:count 3} :timestamp "2017"}]))))

  (testing "timeseries with limit should chop results"
    (is (submaps? [{:count 1} {:count 2}]
                  (from-druid-results
                   {:dimension {:name "__time" :limit 2}}
                   {:queryType "timeseries"}
                   [{:result {:count 1} :timestamp "2015"}
                    {:result {:count 2} :timestamp "2016"}
                    {:result {:count 3} :timestamp "2017"}])))))
