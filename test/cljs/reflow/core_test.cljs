(ns reflow.core-test
  (:require-macros [cljs.test :refer [deftest testing is]]))

(deftest test-pass []
  (is (= 2 2)))
