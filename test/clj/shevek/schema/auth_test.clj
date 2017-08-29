(ns shevek.schema.auth-test
  (:require [clojure.test :refer :all]
            [shevek.schema.auth :refer [filter-cubes filter-cube]]))

(deftest filter-cubes-test
  (let [all-cubes [{:name "c1"} {:name "c2"}]]
    (testing "by default all cubes are visible"
      (is (= ["c1" "c2"] (map :name (filter-cubes nil all-cubes)))))

    (testing "when all cubes are allowed"
      (is (= ["c1" "c2"] (map :name (filter-cubes {:allowed-cubes "all"} all-cubes)))))

    (testing "if a list of allowed-cubes is specified, only those cubes are visible"
      (is (= ["c2"] (map :name (filter-cubes {:allowed-cubes [{:name "c2"}]} all-cubes))))
      (is (= [] (map :name (filter-cubes {:allowed-cubes []} all-cubes)))))))

(defn filtered-measures [cube user]
  (->> (filter-cube cube user) :measures (map :name)))

(deftest filter-cube-test
  (let [cube {:name "c1" :measures [{:name "m1"} {:name "m2"}]}]
    (testing "by default or when 'all' value is specified all measures are visible"
      (is (= ["m1" "m2"] (filtered-measures cube {})))
      (is (= ["m1" "m2"] (filtered-measures cube {:allowed-cubes "all"})))
      (is (= ["m1" "m2"] (filtered-measures cube {:allowed-cubes [{:name "c1" :measures "all"}]}))))

    (testing "if a list of measures is specified for the cube, only those measures are visible"
      (is (= ["m1"] (filtered-measures cube {:allowed-cubes [{:name "c1" :measures ["m1"]}]})))
      (is (= ["m2"] (filtered-measures cube {:allowed-cubes [{:name "c1" :measures ["m2"]}]}))))

    (testing "if the cube is not allowed it should throw unauthorized error"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized"
                            (filter-cube cube {:allowed-cubes [{:name "c2"}]}))))

    (testing "if no measures are allowed an unauthorized error is thrown"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Unauthorized"
                            (filtered-measures cube {:allowed-cubes [{:name "c1" :measures []}]}))))))
