(ns shevek.querying.expression-test
  (:require [clojure.test :refer :all]
            [shevek.querying.expression :refer [eval-expression measure->druid]]))

(deftest eval-expression-test
  (testing "numeric expression should return constant post-aggregator and none aggretators"
    (is (= [{:type "constant" :value 100} []]
           (eval-expression "100"))))

  (testing "aggregation expression should return fieldAccess post-aggregator and one aggregator for the field"
    (is (= [{:type "fieldAccess" :fieldName "t0!amount"}
            [{:fieldName "amount" :type "doubleSum" :name "t0!amount"}]]
           (eval-expression "(sum $amount)"))))

  (testing "aggregation count-distinct"
    (is (= [{:type "fieldAccess" :fieldName "t0!users"}
            [{:fieldName "users" :type "hyperUnique" :name "t0!users"}]]
           (eval-expression "(count-distinct $users)"))))

  (testing "arithmetic expression between a field and a constant"
    (is (= [{:type "arithmetic" :fn "/"
             :fields [{:type "fieldAccess" :fieldName "t0!amount"}
                      {:type "constant" :value 100}]}
            [{:fieldName "amount" :type "doubleSum" :name "t0!amount"}]]
           (eval-expression "(/ (sum $amount) 100)"))))

  (testing "arithmetic expression between two fields"
    (is (= [{:type "arithmetic" :fn "*"
             :fields [{:type "fieldAccess" :fieldName "t0!amount"}
                      {:type "fieldAccess" :fieldName "t1!quantity"}]}
            [{:fieldName "amount" :type "doubleSum" :name "t0!amount"}
             {:fieldName "quantity" :type "doubleSum" :name "t1!quantity"}]]
           (eval-expression "(* (sum $amount) (sum $quantity))"))))

  (testing "nested arithmetic expression"
    (is (= [{:type "arithmetic" :fn "/"
             :fields [{:type "arithmetic" :fn "*"
                       :fields [{:type "fieldAccess" :fieldName "t0!amount"}
                                {:type "constant" :value 5}]}
                      {:type "constant" :value 100}]}
            [{:fieldName "amount" :type "doubleSum" :name "t0!amount"}]]
           (eval-expression "(/ (* (sum $amount) 5) 100)")))))

#_(deftest to-druid-test
    (testing "arithmetic post-aggregator of existing field against a constant value"
      (is (= {:aggregations [{:fieldName "amount" :type "longSum" :name "t1!amount"}]
              :postAggregations [{:type "arithmetic"
                                  :name "amount"
                                  :fn "/"
                                  :fields [{:type "fieldAccess" :fieldName "o1!amount"}
                                           {:type "constant" :value 100}]}]}
            (measure->druid {:name "amount" :type "longSum" :expression "(/ (sum $amount) 100)"}))))

    (testing "arithmetic post-aggregator creating new field refering to other fields"
      (is (= {:aggregations [{:fieldName "unitPrice" :type "doubleSum" :name "t1!unitPrice"}
                             {:fieldName "quantity" :type "longSum" :name "t1!quantity"}]
              :postAggregations [{:type "arithmetic"
                                  :name "total"
                                  :fn "*"
                                  :fields [{:type "fieldAccess" :fieldName "t1!unitPrice"}
                                           {:type "fieldAccess" :fieldName "t1!quantity"}]}]}
            (measure->druid {:name "total" :type "longSum" :expression "(* (sum $unitPrice) (sum $quantity))"})))))
