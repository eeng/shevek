(ns shevek.schema.auth-test
  (:require [clojure.test :refer [deftest testing is]]
            [shevek.schema.auth :refer [filter-cubes filter-cube]]))

(defn filtered-cubes [user cubes]
  (map :name (filter-cubes user cubes)))

(deftest filter-cubes-test
  (let [all-cubes [{:name "c1"} {:name "c2"}]]
    (testing "by default all cubes are visible"
      (is (= ["c1" "c2"] (filtered-cubes nil all-cubes))))

    (testing "when all cubes are allowed"
      (is (= ["c1" "c2"] (filtered-cubes {:allowed-cubes "all"} all-cubes))))

    (testing "if a list of allowed-cubes is specified, only those cubes are visible"
      (is (= ["c2"] (filtered-cubes {:allowed-cubes [{:name "c2"}]} all-cubes)))
      (is (= [] (filtered-cubes {:allowed-cubes []} all-cubes))))

    (testing "should filter the measures of each cube"
      (let [filtered-cubes (filter-cubes {:allowed-cubes [{:name "c1" :measures ["m11"]}
                                                          {:name "c2" :measures ["m22"]}]}
                            [{:name "c1" :measures [{:name "m11"} {:name "m12"}]}
                             {:name "c2" :measures [{:name "m21"} {:name "m22"}]}])]
        (is (= ["m11"] (->> filtered-cubes first :measures (map :name))))
        (is (= ["m22"] (->> filtered-cubes second :measures (map :name))))))))

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

    (testing "if the cube is not allowed it should return the cube without measures"
      (is (= [] (filtered-measures cube {:allowed-cubes [{:name "c2"}]}))))

    (testing "if no measures are allowed an unauthorized error is thrown"
      (is (= [] (filtered-measures cube {:allowed-cubes [{:name "c1" :measures []}]}))))))
