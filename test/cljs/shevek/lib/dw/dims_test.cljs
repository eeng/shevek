(ns shevek.lib.dw.dims-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [shevek.lib.dw.dims :refer [replace-dimension]]))

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
