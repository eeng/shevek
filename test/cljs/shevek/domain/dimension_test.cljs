(ns shevek.domain.dimension-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [shevek.domain.dimension :refer [replace-dimension merge-dimensions]]))

(deftest replace-dimension-test
  (testing "changing a dimension's fields"
    (is (= [{:name "a"} {:name "b" :descending true} {:name "c"}]
           (replace-dimension [{:name "a"} {:name "b"} {:name "c"}] {:name "b" :descending true}))))

  (testing "replacing one dimension by another not already present in the collection"
    (is (= [{:name "c"} {:name "b"}]
           (replace-dimension [{:name "a"} {:name "b"}] {:name "a"} {:name "c"})))
    (is (= [{:name "a"} {:name "c"}]
           (replace-dimension [{:name "a"} {:name "b"}] {:name "b"} {:name "c"}))))

  (testing "replacing one dimension by another already present in the collection switch places"
    (is (= [{:name "b"} {:name "a"}]
           (replace-dimension [{:name "a"} {:name "b"}] {:name "a"} {:name "b"})))
    (is (= [{:name "b"} {:name "a"}]
           (replace-dimension [{:name "a"} {:name "b"}] {:name "b"} {:name "a"})))))

(deftest merge-dimensions-test
  (testing "when the dims to add are all new"
    (is (= [{:name "a"} {:name "b"} {:name "c"}]
           (merge-dimensions [{:name "a"}] [{:name "b"} {:name "c"}]))))

  (testing "when some dim exists in the current collection it should replace it with the new one"
    (is (= [{:name "a" :value 2} {:name "b" :value 3}]
           (merge-dimensions [{:name "a" :value 1}] [{:name "a" :value 2} {:name "b" :value 3}])))))
