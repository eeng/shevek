(ns shevek.engine.druid-native.solver.topn-test
  (:require [clojure.test :refer :all]
            [shevek.asserts :refer [submap? without? submaps?]]
            [shevek.engine.druid-native.solver.topn :refer [to-druid-query from-druid-results]]))

(deftest to-druid-query-test
  (testing "general structure"
    (is (submap? {:queryType "topN"
                  :dataSource "wikiticker"
                  :granularity "all"
                  :dimension "page"
                  :metric {:type "numeric" :metric "count"}
                  :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}]}
                 (to-druid-query
                  {:cube "wikiticker"
                   :dimension {:name "page"}
                   :measures [{:name "count" :expression "(sum $count)"}]}))))

  (testing "limit should be translated to threshold"
    (is (submap? {:threshold 10}
                 (to-druid-query {:dimension {:name "page" :limit 10}}))))

  (testing "sorting"
    (testing "query with one dimension sorting by a selected metric in ascending order"
      (is (submap? {:metric {:type "inverted" :metric {:type "numeric" :metric "added"}}}
                   (to-druid-query
                    {:cube "wikiticker"
                     :dimension {:name "page" :sort-by {:name "added" :descending false :expression "(sum $added)"}}
                     :measures [{:name "count" :expression "(sum $count)"}
                                {:name "added" :expression "(sum $added)"}]}))))

    (testing "query with one dimension sorting by a non selected metric should add it to the aggregations"
      (is (submap? {:metric {:type "numeric" :metric "users"}
                    :aggregations [{:name "count" :fieldName "count" :type "doubleSum"}
                                   {:name "users" :fieldName "users" :type "hyperUnique"}]}
                   (to-druid-query {:dimension {:name "page"
                                                :sort-by {:name "users" :expression "(count-distinct $users)" :measure? true}}
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
                                                :sort-by {:name "aFloat" :descending false :type "FLOAT"}}})))))

  (testing "extraction functions",
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

    (testing "the timeFormat extraction should use the time-zone option if present"
      (is (= {:type "timeFormat" :locale "es" :timeZone "Europe/Berlin"}
             (-> (to-druid-query {:dimension {:name "year" :column "__time"
                                              :extraction [{:type "timeFormat" :locale "es"}]}
                                  :time-zone "Europe/Berlin"})
                 (get-in [:dimension :extractionFn]))))))

  (testing "dimension expressions"
    (testing "when an expression is configured, should add it through a virtual column"
      (let [dq (to-druid-query
                {:dimension {:name "mes"
                             :type "LONG"
                             :expression "timestamp_extract(__time, 'MONTH')"}})]
        (is (= [{:type "expression"
                 :expression "timestamp_extract(__time, 'MONTH')"
                 :outputType "LONG"
                 :name "mes:v"}]
               (:virtualColumns dq)))
        (is (= {:type "default"
                :dimension "mes:v"
                :outputName "mes"
                :outputType "LONG"}
               (:dimension dq)))))

    (testing "when no expression is configured, virtualColumns should be empty"
      (is (= [] (:virtualColumns (to-druid-query {:dimension {:name "product"}})))))

    (testing "a dimension with expression in a sort-by should not be confused with a measure"
      (is (= "topN"
             (:queryType
              (to-druid-query
               {:dimension {:name "mes" :type "LONG"
                            :sort-by {:name "mes"
                                      :expression "timestamp_extract(__time, 'MONTH')"}}})))))

    (testing "when filtering by the same dimension which has an expression"
      (let [dq (to-druid-query
                {:dimension {:name "mes" :expression "timestamp_extract(__time, 'MONTH')" :type "STRING"}
                 :filters [{:name "mes" :operator "include" :value ["Enero"]
                            :expression "timestamp_extract(__time, 'MONTH')"}]})]
        (is (= "mes:v" (-> dq :filter :dimension)))
        (is (= ["mes:v"] (map :name (:virtualColumns dq))))))

    (testing "when filtering by the other dimension which has an expression"
      (let [dq (to-druid-query
                {:dimension {:name "product"}
                 :filters [{:name "mes" :operator "include" :value ["Enero"]
                            :expression "timestamp_extract(__time, 'MONTH')"}]})]
        (is (= "mes:v" (-> dq :filter :dimension)))
        (is (= ["mes:v"] (map :name (:virtualColumns dq)))))))

  (testing "multi-value dimensions"
    (testing "should use a listFiltered dimensionSpec when filtered that dimension with operator include"
      (is (= {:type "listFiltered" :delegate "tags" :values ["t1"]}
             (:dimension (to-druid-query {:dimension {:name "tags" :multi-value true}
                                          :filters [{:name "tags" :operator "include" :value ["t1"]}]})))))

    (testing "should use the default dimensionSpec when not filtered by that dimension or when filtered with exclude"
      (is (= "tags" (:dimension (to-druid-query {:dimension {:name "tags" :multi-value true}}))))
      (is (= "tags" (:dimension (to-druid-query {:dimension {:name "tags" :multi-value true}
                                                 :filters [{:name "tags" :operator "exclude" :value ["t1"]}]})))))))

(deftest from-druid-results-test
  (testing "results normalization"
    (is (submaps? [{:page "P1" :count 1}
                   {:page "P2" :count 2}]
                  (from-druid-results
                   [{:result [{:page "P1" :count 1}
                              {:page "P2" :count 2}]}])))))
