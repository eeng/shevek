(ns shevek.querying.expression-test
  (:require [clojure.test :refer :all]
            [shevek.querying.expression :as e]
            [shevek.engine.druid.planner.common :refer [make-tig]]))

(defn measure->druid [m]
  (e/measure->druid m (make-tig)))

(deftest measure->druid-test
  (testing "aggregators"
    (testing "count aggregator"
      (is (= {:aggregations [{:type "count" :name "x"}]
              :postAggregations []}
             (measure->druid {:name "x" :expression "(count)"}))))

    (testing "sum aggregator"
      (is (= {:aggregations [{:type "doubleSum" :fieldName "x" :name "x"}]
              :postAggregations []}
             (measure->druid {:name "x" :expression "(sum $x)"}))))

    (testing "count-distinct aggregator"
      (is (= {:aggregations [{:type "hyperUnique" :fieldName "x" :name "x"}]
              :postAggregations []}
             (measure->druid {:name "x" :expression "(count-distinct $x)"}))))

    (testing "max aggregator"
      (is (= {:aggregations [{:type "doubleMax" :fieldName "x" :name "x"}]
              :postAggregations []}
             (measure->druid {:name "x" :expression "(max $x)"}))))

    (testing "filtered aggregator"
      (is (= {:aggregations [{:type "filtered"
                              :filter {:type "selector" :dimension "country" :value "ar"}
                              :aggregator {:type "doubleSum" :fieldName "amount" :name "amount"}
                              :name "amount"}]
              :postAggregations []}
             (measure->druid {:name "amount" :expression "(where (= $country \"ar\") (sum $amount))"})))
      (is (= {:aggregations [{:type "filtered"
                              :filter {:type "not"
                                       :field {:type "selector" :dimension "country" :value "ar"}}
                              :aggregator {:type "doubleSum" :fieldName "amount" :name "amount"}
                              :name "amount"}]
              :postAggregations []}
             (measure->druid {:name "amount" :expression "(where (not= $country \"ar\") (sum $amount))"})))
      (is (= {:aggregations [{:type "filtered"
                              :filter {:type "and"
                                       :fields [{:type "selector" :dimension "country" :value 1}
                                                {:type "selector" :dimension "city" :value 2}]}
                              :aggregator {:type "doubleSum" :fieldName "amount" :name "amount"}
                              :name "amount"}]
              :postAggregations []}
             (measure->druid {:name "amount" :expression "(where (and (= $country 1) (= $city 2)) (sum $amount))"}))))

    (testing "filtered aggregator with sintax sugar"
      (is (= {:aggregations [{:type "filtered"
                              :filter {:type "selector" :dimension "x" :value 1}
                              :aggregator {:type "doubleSum" :fieldName "amount" :name "amount"}
                              :name "amount"}]
              :postAggregations []}
             (measure->druid {:name "amount" :expression "(where {$x 1} (sum $amount))"})))
      (is (= {:aggregations [{:type "filtered"
                              :filter {:type "and"
                                       :fields [{:type "selector" :dimension "x" :value 1}
                                                {:type "selector" :dimension "y" :value 2}]}
                              :aggregator {:type "doubleSum" :fieldName "amount" :name "amount"}
                              :name "amount"}]
              :postAggregations []}
             (measure->druid {:name "amount" :expression "(where {$x 1 $y 2} (sum $amount))"})))))

  (testing "post-aggregators"
    (testing "arithmetic operation between same measure and a constant"
      (is (= {:aggregations [{:type "doubleSum" :fieldName "amount" :name "_t0"}]
              :postAggregations [{:type "arithmetic" :name "amount" :fn "/"
                                  :fields [{:type "fieldAccess" :fieldName "_t0"}
                                           {:type "constant" :value 100}]}]}
             (measure->druid {:name "amount" :expression "(/ (sum $amount) 100)"}))))

    (testing "arithmetic operation between other measures"
      (is (= {:aggregations [{:fieldName "unitPrice" :type "doubleSum" :name "_t0"}
                             {:fieldName "quantity" :type "doubleSum" :name "_t1"}]
              :postAggregations [{:type "arithmetic" :name "total" :fn "*"
                                  :fields [{:type "fieldAccess" :fieldName "_t0"}
                                           {:type "fieldAccess" :fieldName "_t1"}]}]}
             (measure->druid {:name "total" :expression "(* (sum $unitPrice) (sum $quantity))"}))))

    (testing "complex nested filtered expression"
      (is (= {:aggregations [{:type "filtered"
                              :filter {:type "selector" :dimension "country" :value "br"}
                              :aggregator {:type "doubleSum" :fieldName "amount" :name "_t0"}
                              :name "_t0"}
                             {:type "doubleSum" :fieldName "units" :name "_t1"}]
              :postAggregations [{:type "arithmetic" :fn "/" :name "x"
                                  :fields [{:type "arithmetic" :fn "*" :name nil
                                            :fields [{:type "fieldAccess" :fieldName "_t0"}
                                                     {:type "fieldAccess" :fieldName "_t1"}]}
                                           {:type "constant" :value 100}]}]}
             (measure->druid {:name "x" :expression "(/ (* (where (= $country \"br\") (sum $amount)) (sum $units)) 100)"}))))

    (testing "greatest post aggregator"
      (is (= {:aggregations []
              :postAggregations [{:type "doubleGreatest" :name "amount"
                                  :fields [{:type "constant" :value 100}
                                           {:type "constant" :value 200}]}]}
             (measure->druid {:name "amount" :expression "(greatest 100 200)"}))))

    (testing "javascript post aggregator"
      (is (= {:aggregations [{:type "doubleSum" :fieldName "delta" :name "_t0"}
                             {:type "doubleSum" :fieldName "total" :name "_t1"}]
              :postAggregations [{:type "javascript" :name "amount" :fieldNames ["_t0" "_t1"]
                                  :function "function(f1, f2) { ... }"}]}
             (measure->druid {:name "amount"
                              :expression "(js [(sum $delta) (sum $total)] \"function(f1, f2) { ... }\")"}))))

    (testing "hyperUniqueCardinality post aggregator should be used instead of fieldAccess when hyperUnique agggregator are used"
      (is (= {:aggregations [{:type "hyperUnique" :fieldName "users" :name "_t0"}
                             {:type "filtered"
                              :filter {:type "selector" :dimension "x" :value "true"}
                              :aggregator {:type "hyperUnique" :fieldName "users" :name "_t1"}
                              :name "_t1"}]
              :postAggregations [{:type "arithmetic" :name "amount" :fn "/"
                                  :fields [{:type "hyperUniqueCardinality" :fieldName "_t0"}
                                           {:type "hyperUniqueCardinality" :fieldName "_t1"}]}]}
             (measure->druid {:name "amount" :expression "(/ (count-distinct $users) (where {$x \"true\"} (count-distinct $users)))"}))))))
