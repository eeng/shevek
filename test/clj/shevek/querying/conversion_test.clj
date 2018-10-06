(ns shevek.querying.conversion-test
  (:require [clojure.test :refer :all]
            [shevek.asserts :refer [submap? without? submaps?]]
            [shevek.querying.conversion :refer [to-druid-query from-druid-results]]))

(deftest to-druid-query-test
  (testing "query types"
    (testing "totals query"
      (is (submap? {:queryType "timeseries"
                    :dataSource "wikiticker"
                    :granularity "all"
                    :intervals "2015/2016"
                    :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}]}
                   (to-druid-query {:cube "wikiticker"
                                    :measures [{:name "count" :expression "(sum $count)"}]
                                    :filters [{:interval ["2015" "2016"]}]}))))

    (testing "query with one atemporal dimension should generate a topN query"
      (is (submap? {:queryType "topN"
                    :dataSource "wikiticker"
                    :granularity "all"
                    :dimension "page"
                    :metric {:type "numeric" :metric "count"}
                    :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}]}
                   (to-druid-query {:cube "wikiticker"
                                    :dimension {:name "page"}
                                    :measures [{:name "count" :expression "(sum $count)"}]}))))

    (testing "query with one time dimension should generate a timeseries query with the specified granularity, sort order and timezone (default to local)"
      (is (submap? {:queryType "timeseries"
                    :intervals "2015/2016"
                    :granularity {:type "period" :period "P1D" :timeZone "America/Argentina/Buenos_Aires"}
                    :descending true}
                   (to-druid-query {:filters [{:interval ["2015" "2016"]}]
                                    :dimension {:name "__time" :granularity "P1D" :sort-by {:descending true}}})))
      (is (submap? {:granularity {:type "period" :period "P1D" :timeZone "Europe/Paris"}}
                   (to-druid-query {:dimension {:name "__time" :granularity "P1D"}
                                    :time-zone "Europe/Paris"}))))

    (testing "ordering by other dimension should use a groupBy query"
      (let [dq (to-druid-query
                {:dimension {:name "monthName"
                             :sort-by {:name "monthNum" :descending false :type "LONG"}}})]
        (is (submap? {:queryType "groupBy"
                      :dimensions ["monthNum", "monthName"]
                      :granularity "all"}
                     dq))
        (is (without? :metric dq))
        (is (without? :dimension dq)))))

  (testing "filter"
    (testing ":is operator"
      (is (submap? {:filter {:type "selector"
                             :dimension "isRobot"
                             :value "true"}}
                   (to-druid-query {:filters [{:name "isRobot" :operator "is" :value "true"}]})))
      (is (submap? {:filter {:type "selector"
                             :dimension "countryName"
                             :value nil}}
                   (to-druid-query {:filters [{:name "countryName" :operator "is" :value nil}]}))))

    (testing "two filters should generate an 'and' filter"
      (is (submap? {:filter {:type "and"
                             :fields [{:type "selector" :dimension "isRobot" :value "true"}
                                      {:type "selector" :dimension "isNew" :value "false"}]}}
                   (to-druid-query {:filters [{:name "__time"} ; Ignored because it's send in the interval
                                              {:name "isRobot" :operator "is" :value "true"}
                                              {:name "isNew" :operator "is" :value "false"}]})))
      (is (without? :filter (to-druid-query {:filters [{:name "isRobot"} {:name "isNew"}]})))
      (is (without? :filter (to-druid-query {:filters [{:name "isRobot" :operator "include" :value []}]}))))

    (testing ":include operator"
      (is (submap? {:filter {:type "in"
                             :dimension "countryName"
                             :values ["Italy" "France"]}}
                   (to-druid-query {:filters [{:name "countryName" :operator "include" :value ["Italy" "France"]}]}))))

    (testing ":exclude operator"
      (is (submap? {:filter {:type "not"
                             :field {:type "in"
                                     :dimension "countryName"
                                     :values [nil "France"]}}}
                   (to-druid-query {:filters [{:name "countryName" :operator "exclude" :value [nil "France"]}]}))))

    (testing ":search operator"
      (is (submap? {:filter {:type "search"
                             :dimension "countryName"
                             :query {:type "insensitive_contains"
                                     :value "arg"}}}
                   (to-druid-query {:filters [{:name "countryName" :operator "search" :value "arg"}]})))))

  (testing "sorting"
    (testing "query with one dimension sorting by a selected metric in ascending order"
      (is (submap? {:queryType "topN"
                    :metric {:type "inverted" :metric {:type "numeric" :metric "added"}}}
                   (to-druid-query {:cube "wikiticker"
                                    :dimension {:name "page" :sort-by {:name "added" :descending false :expression "(sum $added)"}}
                                    :measures [{:name "count" :expression "(sum $count)"}
                                               {:name "added" :expression "(sum $added)"}]}))))

    (testing "query with one dimension sorting by a non selected metric should add it to the aggregations"
      (is (submap? {:queryType "topN"
                    :metric {:type "numeric" :metric "users"}
                    :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}
                                   {:name "users" :fieldName "users" :type "hyperUnique"}]}
                   (to-druid-query {:dimension {:name "page"
                                                :sort-by {:name "users" :expression "(count-distinct $users)"}}
                                    :measures [{:name "count" :expression "(sum $count)"}]}))))

    (testing "ascending ordered by the same dimension should use lexicographic sorting"
      (is (submap? {:metric {:type "dimension" :ordering "lexicographic"}}
                   (to-druid-query {:dimension {:name "page" :sort-by {:name "page" :descending false}}})))
      (is (submap? {:metric {:type "dimension" :ordering "lexicographic"}}
                   (to-druid-query {:dimension {:name "isNew" :type "BOOL" :sort-by {:name "isNew" :descending false}}}))))

    (testing "descending ordered by the same dimension should use lexicographic sorting"
      (is (submap? {:metric {:type "inverted"
                             :metric {:type "dimension" :ordering "lexicographic"}}}
                   (to-druid-query {:dimension {:name "page" :sort-by {:name "page" :descending true}}}))))

    (testing "ordering by numeric dimensions should use numeric sorting"
      (is (submap? {:metric {:type "dimension" :ordering "numeric"}}
                   (to-druid-query {:dimension {:name "aLong"
                                                :sort-by {:name "aLong" :descending false :type "LONG"}}})))
      (is (submap? {:metric {:type "dimension" :ordering "numeric"}}
                   (to-druid-query {:dimension {:name "aFloat"
                                                :sort-by {:name "aFloat" :descending false :type "FLOAT"}}}))))

    (testing "descending by other dimension"
      (is (submap? {:queryType "groupBy"
                    :limitSpec {:type "default"
                                :limit 100
                                :columns [{:dimension "monthNum"
                                           :direction "descending"
                                           :dimensionOrder "numeric"}]}}
                   (to-druid-query
                    {:dimension {:name "monthName"
                                 :sort-by {:name "monthNum" :descending true :type "LONG"}}})))))

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
              :extractionFn {:type "substring" :index 3}}
             (:dimension
               (to-druid-query {:dimension {:name "year" :column "__time"
                                            :extraction [{:type "substring" :index 3}]}})))))

    (testing "derived dimension with two extraction fns"
      (is (= {:type "extraction" :outputName "year" :dimension "__time"
              :extractionFn {:type "cascade" :extractionFns [{:type "subsring"}
                                                             {:type "strlen"}]}}
             (:dimension
              (to-druid-query {:dimension {:name "year" :column "__time"
                                           :extraction [{:type "subsring"}
                                                        {:type "strlen"}]}})))))

    (testing "should use the dimension extraction function in filters also"
      (is (= {:type "selector" :dimension "__time" :value "2017"
              :extractionFn {:type "strlen"}}
             (:filter
              (to-druid-query {:filters [{:name "year" :operator "is" :value "2017" :column "__time"
                                          :extraction [{:type "strlen"}]}]})))))

    (testing "the timeFormat extraction should use the time-zone option if present"
      (is (= {:type "timeFormat" :locale "es" :timeZone "Europe/Berlin"}
             (-> (to-druid-query {:dimension {:name "year" :column "__time"
                                              :extraction [{:type "timeFormat" :locale "es"}]}
                                  :time-zone "Europe/Berlin"})
                 (get-in [:dimension :extractionFn]))))))

  (testing "multi-value dimensions"
    (testing "should use a listFiltered dimensionSpec when filtered that dimension with operator include"
      (is (= {:type "listFiltered" :delegate "tags" :values ["t1"]}
             (:dimension (to-druid-query {:dimension {:name "tags" :multi-value true}
                                          :filters [{:name "tags" :operator "include" :value ["t1"]}]})))))

    (testing "should use the default dimensionSpec when not filtered by that dimension or when filtered with exclude"
      (is (= "tags" (:dimension (to-druid-query {:dimension {:name "tags" :multi-value true}}))))
      (is (= "tags" (:dimension (to-druid-query {:dimension {:name "tags" :multi-value true}
                                                 :filters [{:name "tags" :operator "exclude" :value ["t1"]}]}))))))

  (testing "limit"
    (testing "topN queries add a threshold"
      (is (submap? {:queryType "topN" :threshold 10}
                   (to-druid-query {:dimension {:name "page" :limit 10}}))))

    (testing "groupBy queries add a limitSpec"
      (let [dq (to-druid-query
                {:dimension {:sort-by {:name "monthNum" :descending false :type "LONG"}
                             :limit 5}})]
        (is (submap? {:type "default" :limit 5}
                     (:limitSpec dq)))
        (is (without? :threshold dq)))))

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
                   {:__time "2016" :count 2}]
                  (from-druid-results
                   {:dimension {:name "__time" :limit 2}}
                   {:queryType "timeseries"}
                   [{:result {:count 1} :timestamp "2015"}
                    {:result {:count 2} :timestamp "2016"}
                    {:result {:count 3} :timestamp "2017"}]))))

  (testing "groupBy"
    (is (submaps? [{:countryName "Albania" :countryIsoCode "AL" :count 2}
                   {:countryName "Angola" :countryIsoCode "AO" :count 4}]
                  (from-druid-results
                   {:dimension {:name "countryName" :limit 2}}
                   {:queryType "groupBy"}
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
