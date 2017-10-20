(ns shevek.querying.auth-test
  (:require [clojure.test :refer :all]
            [shevek.makers :refer [make]]
            [shevek.querying.auth :refer [filter-query]]
            [shevek.schemas.query :refer [Query]]))

(deftest filter-query-test
  (testing "if the user doesn't have filters configured for the cube should return the same filters"
    (let [q (make Query {:cube "C"})]
      (is (= q (filter-query {:allowed-cubes "all"} q)))
      (is (= q (filter-query {:allowed-cubes [{:name "C"}]} q)))
      (is (= (:filters q) (:filters (filter-query {:allowed-cubes [{:name "D" :filters [{:name "F" :value "f"}]}]} q))))))

  (testing "if the user have filters for the query's cube, should combined them with the ones in the query"
    (let [q1 {:cube "C1" :filters [{:name "T" :interval ["2017" "2018"]}]}
          q2 {:cube "C2" :filters [{:name "T" :interval ["2018" "2019"]}]}
          user {:allowed-cubes [{:name "C1" :filters [{:name "F1" :value "f1"}]}
                                {:name "C2" :filters [{:name "F2" :value "f2"}]}]}]
      (is (= [{:name "T" :interval ["2017" "2018"]} {:name "F1" :value "f1"}]
             (:filters (filter-query user q1))))
      (is (= [{:name "T" :interval ["2018" "2019"]} {:name "F2" :value "f2"}]
             (:filters (filter-query user q2))))))

  (testing "should filter not allowed measures"
    (let [q {:cube "c1" :measures [{:name "m1"} {:name "m2"}]}]
      (is (= ["m2"]
             (->> (filter-query {:allowed-cubes [{:name "c1" :measures ["m2"]}]} q)
                  :measures (map :name))))
      (is (= []
             (->> (filter-query {:allowed-cubes [{:name "c2" :measures "all"}]} q)
                  :measures (map :name)))))))
