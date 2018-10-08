(ns shevek.engine.druid.planner.common-test
  (:require [clojure.test :refer :all]
            [shevek.asserts :refer [submap? without? submaps?]]
            [shevek.engine.druid.planner.timeseries :as timeseries :refer [to-druid-query]]))

(deftest common-fields-to-all-druid-queries
  (testing "filters"
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

  (testing "timeout"
    (is (= 30000 (get-in (to-druid-query {}) [:context :timeout])))))
