(ns shevek.schemas.conversion-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [pjstadig.humane-test-output]
            [shevek.asserts :refer [submap? submaps? without?]]
            [shevek.schemas.conversion :refer [viewer->report report->viewer viewer->query]]
            [shevek.lib.time :refer [date-time]]))

(defn viewer [fields]
  (merge {:cube {:name "c"}
          :filters [{:name "__time" :period "latest-day"}]
          :measures [{:name "m1"}]
          :viztype :totals
          :pinboard {:measure {:name "m1"} :dimensions []}}
         fields))

(deftest viewer->report-tests
  (testing "should store only the cube name"
    (is (= "wikiticker" (-> {:cube {:name "wikiticker"}} viewer viewer->report :cube))))

  (testing "should store only the selected measures names"
    (is (= ["added" "deleted"]
           (-> {:measures [{:name "added"} {:name "deleted"}]}
               viewer viewer->report :measures))))

  (testing "in each filter should store only the dimension name besides its own fields, dates and sets"
    (is (= [{:name "time" :period "current-day"}
            {:name "page" :operator "exclude" :value [nil]}]
           (-> {:filters [{:name "time" :type "..." :period "current-day"}
                          {:name "page" :type "..." :operator "exclude" :value #{nil}}]}
               viewer viewer->report :filters)))
    (is (= [{:interval ["2018-04-04T03:00:00.000Z" "2018-04-06T02:59:59.999Z"]}]
           (-> {:filters [{:interval [(date-time 2018 4 4) (date-time 2018 4 5)]}]}
               viewer viewer->report :filters))))

  (testing "in each split should store only the dimension name besides its own fields"
    (is (= [{:name "page" :limit 10 :sort-by {:name "page" :descending true}}
            {:name "time" :granularity "P1D" :sort-by {:name "count" :descending false}}]
           (-> {:splits [{:name "page" :type "t" :limit 10
                          :sort-by {:name "page" :type "t" :descending true}}
                         {:name "time" :type "t" :granularity "P1D" :column "..." :extraction "..."
                          :sort-by {:name "count" :type "t" :expression "e" :format "f" :descending false :favorite true}}]}
               viewer viewer->report :splits))))

  (testing "should convert the pinboard"
    (is (= {:measure "count"
            :dimensions [{:name "time" :granularity "PT1H" :sort-by {:name "time" :descending false}}]}
           (-> {:pinboard {:measure {:name "count" :type "..."}
                           :dimensions [{:name "time" :type "..." :granularity "PT1H"
                                         :sort-by {:name "time" :title "..." :descending false}}]}}
               viewer viewer->report :pinboard))))

  (testing "should not store user-id as the URL"
    (is (without? :user-id (viewer->report (viewer {})))))

  (testing "should convert the viztype"
    (is (= "pie-chart" (:viztype (viewer->report (viewer {:viztype :pie-chart})))))))

(deftest report->viewer-tests
  (testing "should convert back to dates and sets and add title and other fields"
    (is (= [{:name "time" :title "Fecha" :period "current-day"}
            {:name "time2" :interval [(date-time 2018 4 4) (date-time 2018 4 5)]}
            {:name "page" :title "Pag" :operator "exclude" :value #{nil}}]
           (-> {:filters [{:name "time" :period "current-day"}
                          {:name "time2" :interval ["2018-04-04T03:00:00.000Z" "2018-04-05T03:00:00.000Z"]}
                          {:name "page" :operator "exclude" :value [nil]}]}
               (report->viewer {:dimensions [{:name "time" :title "Fecha"}
                                             {:name "page" :title "Pag"}]})
               :filters))))

  (testing "should converted back the pinboard with the info in the cube"
    (is (= {:measure {:name "count" :type "longSum"}
            :dimensions [{:name "time" :title "Time"}]}
           (-> {:pinboard {:measure "count"
                           :dimensions [{:name "time"}]}}
               (report->viewer {:dimensions [{:name "otherD" :title "..."} {:name "time" :title "Time"}]
                                :measures [{:name "otherM" :type "..."} {:name "count" :type "longSum"}]})
               :pinboard))))

  (testing "if the pinboard measure in the report doesn't exist on the cube (because the user shouldn't see it) should use the first available measure"
    (is (= {:measure {:name "count" :type "longSum"} :dimensions []}
           (-> {:pinboard {:measure "secretAmount"}}
               (report->viewer {:measures [{:name "count" :type "longSum"}]})
               :pinboard))))

  (testing "should convert back the viztype"
    (is (= :pie-chart (:viztype (report->viewer {:viztype "pie-chart"} {})))))

  (testing "if a report measure is not present on the cube (because modified permissions) should remove it"
    (is (= [{:name "m2" :title "M2"}]
           (-> {:measures ["m1" "m2"]}
               (report->viewer {:measures [{:name "m2" :title "M2"}]})
               :measures)))))

(deftest viewer->query-tests
  (testing "should simplify the cube and measures"
    (is (= "c" (:cube (viewer->query (viewer {:cube {:name "c"}})))))
    (is (= ["m1" "m2"] (:measures (viewer->query (viewer {:measures [{:name "m1"} {:name "m2"}]}))))))

  (testing "should stringify the interval"
    (is (= [{:interval ["2018-01-01T03:00:00.000Z" "2019-01-02T02:59:59.999Z"]}]
           (:filters (viewer->query (viewer {:filters [{:interval [(date-time 2018) (date-time 2019)]}]}))))))

  (testing "should remove unneeded keys from filters and splits"
    (is (= [{:period "latest-day"}
            {:name "d" :operator "include" :value ["v"]}]
           (:filters (viewer->query (viewer {:filters [{:period "latest-day" :title "Fecha"}
                                                       {:name "d" :operator "include" :value #{"v"} :title "D"}]})))))))
