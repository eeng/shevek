(ns shevek.reports.repository-test
  (:require [clojure.test :refer :all]
            [shevek.makers :refer [make]]
            [shevek.asserts :refer [submap? submaps?]]
            [shevek.reports.repository :refer [viewer->report]])
  (:import [org.bson.types ObjectId]))

(deftest viewer->report-tests
  (testing "should store only the cube id"
    (let [cube-id (ObjectId.)]
      (is (= cube-id
             (-> {:cube {:_id cube-id}} viewer->report :cube)))))

  (testing "should store only the selected measures names"
    (is (= ["added" "deleted"]
           (-> {:measures [{:name "added"} {:name "deleted"}]}
               viewer->report :measures))))

  (testing "in each filter should store only the dimension name besides its own fields"
    (is (submaps? [{:dimension "time" :selected-period :current-day}
                   {:dimension "page" :operator "exclude" :value #{nil}}]
                  (-> {:filter [{:dimension {:name "time"} :selected-period :current-day}
                                {:dimension {:name "page"} :operator "exclude" :value #{nil}}]}
                      viewer->report :filter))))

  (testing "in each split should store only the dimension name besides its own fields"
    (is (submaps? [{:dimension "page" :limit 10 :sort-by {:dimension "page" :descending true}}
                   {:dimension "time" :granularity "P1D" :sort-by {:measure "count" :descending false}}]
                  (-> {:split [{:dimension {:name "page"} :limit 10
                                :sort-by {:dimension {:name "page"} :descending true}}
                               {:dimension {:name "time"} :granularity "P1D"
                                :sort-by {:measure {:name "count"} :descending false}}]}
                      viewer->report :split)))))
