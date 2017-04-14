(ns shevek.schemas.conversion-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [pjstadig.humane-test-output]
            [shevek.asserts :refer [submap? submaps?]]
            [shevek.schemas.conversion :refer [viewer->report]]))

(deftest viewer->report-tests
  (testing "should store only the cube id"
    (is (= "wikiticker" (-> {:cube {:name "wikiticker"}} viewer->report :cube))))

  (testing "should store only the selected measures names"
    (is (= ["added" "deleted"]
           (-> {:measures [{:name "added"} {:name "deleted"}]}
               viewer->report :measures))))

  (testing "in each filter should store only the dimension name besides its own fields"
    (is (= [{:name "time" :selected-period "current-day"}
            {:name "page" :operator "exclude" :value #{nil}}]
           (-> {:filter [{:name "time" :type "..." :selected-period :current-day}
                         {:name "page" :type "..." :operator "exclude" :value #{nil}}]}
               viewer->report :filter))))

  (testing "in each split should store only the dimension name besides its own fields"
    (is (submaps? [{:name "page" :limit 10 :sort-by {:name "page" :descending true}}
                   {:name "time" :granularity "P1D" :sort-by {:name "count" :descending false}}]
                  (-> {:split [{:name "page" :type "..." :limit 10
                                :sort-by {:name "page" :type "..." :descending true}}
                               {:name "time" :type "..." :granularity "P1D"
                                :sort-by {:name "count" :type "..." :descending false}}]}
                      viewer->report :split)))))
