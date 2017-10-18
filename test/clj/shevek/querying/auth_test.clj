(ns shevek.querying.auth-test
  (:require [clojure.test :refer :all]
            [shevek.makers :refer [make]]
            [shevek.querying.auth :refer [filter-query]]
            [shevek.schemas.query :refer [Query]]))

(deftest filter-query-test
  (testing "if the user doesn't have filters configured for the cube should return the query unchanged"
    (let [q (make Query {:cube "C"})]
      (is (= q (filter-query {:allowed-cubes "all"} q)))
      (is (= q (filter-query {:allowed-cubes [{:name "C" :filters []}]} q)))
      (is (= q (filter-query {:allowed-cubes [{:name "D" :filters [{:name "F" :value "f"}]}]} q)))))

  (testing "if the user have filters for the query's cube, should combined them with the ones in the query"
    (let [q1 (make Query {:cube "C1" :filters [{:name "T" :interval ["2017" "2018"]}]})
          q2 (make Query {:cube "C2" :filters [{:name "T" :interval ["2018" "2019"]}]})
          user {:allowed-cubes [{:name "C1" :filters [{:name "F1" :value "f1"}]}
                                {:name "C2" :filters [{:name "F2" :value "f2"}]}]}]
      (is (= [{:name "T" :interval ["2017" "2018"]} {:name "F1" :value "f1"}]
             (:filters (filter-query user q1))))
      (is (= [{:name "T" :interval ["2018" "2019"]} {:name "F2" :value "f2"}]
             (:filters (filter-query user q2)))))))
