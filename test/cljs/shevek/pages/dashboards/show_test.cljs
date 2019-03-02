(ns shevek.pages.dashboards.show-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [shevek.pages.dashboards.show :refer [add-missing-grid-positions]]))

(deftest add-missing-grid-positions-tests
  (testing "when none of the panels has a grid-pos"
    (is (= [{:x 0 :y 99 :w 12 :h 10} {:x 12 :y 99 :w 12 :h 10} {:x 24 :y 99 :w 12 :h 10} {:x 0 :y 99 :w 12 :h 10}]
           (->> (add-missing-grid-positions [{} {} {} {}])
                (map :grid-pos)))))

  (testing "when some panels have grid-pos"
    (is (= [{:x 0 :y 0 :w 4 :h 4} {:x 4 :y 99 :w 12 :h 10}]
           (->> (add-missing-grid-positions [{:grid-pos {:x 0 :y 0 :w 4 :h 4}} {}])
                (map :grid-pos))))))
