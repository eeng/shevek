(ns shevek.schemas.conversion-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [pjstadig.humane-test-output]
            [shevek.asserts :refer [submap? submaps?]]
            [shevek.schemas.conversion :refer [viewer->report report->viewer]]))

(deftest viewer->report-tests
  (testing "should store only the cube id"
    (is (= "wikiticker" (-> {:cube {:name "wikiticker"}} viewer->report :cube))))

  (testing "should store only the selected measures names"
    (is (= ["added" "deleted"]
           (-> {:measures [{:name "added"} {:name "deleted"}]}
               viewer->report :measures))))

  (testing "in each filter should store only the dimension name besides its own fields and converted keywords and sets"
    (is (= [{:name "time" :selected-period "current-day"}
            {:name "page" :operator "exclude" :value [nil]}]
           (-> {:filter [{:name "time" :type "..." :selected-period :current-day}
                         {:name "page" :type "..." :operator "exclude" :value #{nil}}]}
               viewer->report :filter))))

  (testing "in each split should store only the dimension name besides its own fields"
    (is (submaps? [{:name "page" :limit 10 :sort-by {:name "page" :descending true}}
                   {:name "time" :granularity "P1D" :sort-by {:name "count" :descending false}}]
                  (-> {:split [{:name "page" :type "t" :limit 10
                                :sort-by {:name "page" :type "t" :descending true}}
                               {:name "time" :type "t" :granularity "P1D"
                                :sort-by {:name "count" :type "t" :expression "e" :format "f" :descending false}}]}
                      viewer->report :split))))

  (testing "should convert the pinboard"
    (is (= {:measure "count"
            :dimensions [{:name "time" :granularity "PT1H" :sort-by {:name "time" :descending false}}]}
           (-> {:pinboard {:measure {:name "count" :type "..."}
                           :dimensions [{:name "time" :type "..." :granularity "PT1H"
                                         :sort-by {:name "time" :title "..." :descending false}}]}}
               viewer->report :pinboard)))))

(deftest report->viewer-tests
  (testing "should convert back to keywords and sets and add title and other fields"
    (is (= [{:name "time" :title "Fecha" :selected-period :current-day}
            {:name "page" :title "Pag" :operator "exclude" :value #{nil}}]
           (-> {:filter [{:name "time" :selected-period "current-day"}
                         {:name "page" :operator "exclude" :value [nil]}]}
               (report->viewer {:dimensions [{:name "time" :title "Fecha"}
                                             {:name "page" :title "Pag"}]})
               :filter))))

  (testing "should converted back the pinboard with the info in the cube"
    (is (= {:measure {:name "count" :type "longSum"}
            :dimensions [{:name "time" :title "Time"}]}
           (-> {:pinboard {:measure "count"
                           :dimensions [{:name "time"}]}}
               (report->viewer {:dimensions [{:name "otherD" :title "..."} {:name "time" :title "Time"}]
                                :measures [{:name "otherM" :type "..."} {:name "count" :type "longSum"}]})
               :pinboard)))))
