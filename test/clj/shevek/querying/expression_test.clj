(ns shevek.querying.expression-test
  (:require [clojure.test :refer :all]
            [shevek.querying.expression :as e]
            [shevek.querying.conversion :refer [make-tig]]))

(defn measure->druid [m]
  (e/measure->druid m (make-tig)))

(deftest measure->druid-test
  (testing "aggregators"
    (testing "sum aggregator"
      (is (= {:aggregations [{:type "doubleSum" :fieldName "amount" :name "amount"}]
              :postAggregations []}
             (measure->druid {:name "amount" :expression "(sum $amount)"})))))

  (testing "count-distinct aggregator"
    (is (= {:aggregations [{:type "hyperUnique" :fieldName "amount" :name "amount"}]
            :postAggregations []}
           (measure->druid {:name "amount" :expression "(count-distinct $amount)"}))))

  (testing "filtered aggregator"
    (is (= {:aggregations [{:type "filtered"
                            :filter {:type "selector" :dimension "country" :value "ar"}
                            :aggregator {:type "doubleSum" :fieldName "amount" :name "amount"}
                            :name "amount"}]
            :postAggregations []}
           (measure->druid {:name "amount" :expression "(where (= $country \"ar\") (sum $amount))"}))))

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
                                  :fields [{:type "arithmetic" :fn "*"
                                            :fields [{:type "fieldAccess" :fieldName "_t0"}
                                                     {:type "fieldAccess" :fieldName "_t1"}]}
                                           {:type "constant" :value 100}]}]}
             (measure->druid {:name "x" :expression "(/ (* (where (= $country \"br\") (sum $amount)) (sum $units)) 100)"}))))))
