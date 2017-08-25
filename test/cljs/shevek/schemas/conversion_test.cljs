(ns shevek.schemas.conversion-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [pjstadig.humane-test-output]
            [shevek.asserts :refer [submap? submaps? without?]]
            [shevek.schemas.conversion :refer [viewer->report report->viewer]]
            [shevek.lib.dates :refer [date-time]]))

(deftest viewer->report-tests
  (testing "should store only the cube name"
    (is (= "wikiticker" (-> {:cube {:name "wikiticker"}} viewer->report :cube))))

  (testing "should store only the selected measures names"
    (is (= ["added" "deleted"]
           (-> {:measures [{:name "added"} {:name "deleted"}]}
               viewer->report :measures))))

  (testing "in each filter should store only the dimension name besides its own fields and converted keywords, dates and sets"
    (is (= [{:name "time" :period "current-day"}
            {:name "time2" :interval ["2018-04-04T03:00:00.000Z" "2018-04-05T03:00:00.000Z"]}
            {:name "page" :operator "exclude" :value [nil]}]
           (-> {:filter [{:name "time" :type "..." :period :current-day}
                         {:name "time2" :interval [(date-time 2018 4 4) (date-time 2018 4 5)]}
                         {:name "page" :type "..." :operator "exclude" :value #{nil}}]}
               viewer->report :filter))))

  (testing "in each split should store only the dimension name besides its own fields"
    (is (= [{:name "page" :limit 10 :sort-by {:name "page" :descending true}}
            {:name "time" :granularity "P1D" :sort-by {:name "count" :descending false}}]
           (-> {:split [{:name "page" :type "t" :limit 10
                         :sort-by {:name "page" :type "t" :descending true}}
                        {:name "time" :type "t" :granularity "P1D" :column "..." :extraction "..."
                         :sort-by {:name "count" :type "t" :expression "e" :format "f" :descending false :favorite true}}]}
               viewer->report :split))))

  (testing "should convert the pinboard"
    (is (= {:measure "count"
            :dimensions [{:name "time" :granularity "PT1H" :sort-by {:name "time" :descending false}}]}
           (-> {:pinboard {:measure {:name "count" :type "..."}
                           :dimensions [{:name "time" :type "..." :granularity "PT1H"
                                         :sort-by {:name "time" :title "..." :descending false}}]}}
               viewer->report :pinboard))))

  (testing "should not store user-id as the URL"
    (is (without? :user-id (viewer->report {}))))

  (testing "should convert the viztype"
    (is (= "pie-chart" (:viztype (viewer->report {:viztype :pie-chart}))))))

(deftest report->viewer-tests
  (testing "should convert back to keywords, dates and sets and add title and other fields"
    (is (= [{:name "time" :title "Fecha" :period :current-day}
            {:name "time2" :interval [(date-time 2018 4 4) (date-time 2018 4 5)]}
            {:name "page" :title "Pag" :operator "exclude" :value #{nil}}]
           (-> {:filter [{:name "time" :period "current-day"}
                         {:name "time2" :interval ["2018-04-04T03:00:00.000Z" "2018-04-05T03:00:00.000Z"]}
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
               :pinboard))))

  (testing "should convert back the viztype"
    (is (= :pie-chart (:viztype (report->viewer {:viztype "pie-chart"} {}))))))
