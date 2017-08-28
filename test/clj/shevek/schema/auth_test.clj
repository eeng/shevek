(ns shevek.schema.auth-test
  (:require [clojure.test :refer :all]
            [shevek.schema.auth :refer [filter-visible-cubes]]))

(deftest filter-visible-cubes-test
  (let [all-cubes [{:name "c1"} {:name "c2"}]]
    (testing "by default all cubes are visible"
      (is (= ["c1" "c2"] (map :name (filter-visible-cubes nil all-cubes)))))

    (testing "when all cubes are allowed"
      (is (= ["c1" "c2"] (map :name (filter-visible-cubes {:allowed-cubes "all"} all-cubes)))))

    (testing "if a list of allowed-cubes is specified, only those cubes are visible"
      (is (= ["c2"] (map :name (filter-visible-cubes {:allowed-cubes [{:name "c2"}]} all-cubes))))
      (is (= [] (map :name (filter-visible-cubes {:allowed-cubes []} all-cubes)))))))
