(ns shevek.schemas.conversion-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [shevek.asserts :refer [without?]]
            [shevek.schemas.conversion :refer [designer->report report->designer]]
            [shevek.lib.time :as t]))

(defn designer [fields]
  (merge {:report {:cube "c"}
          :filters [{:name "__time" :period "latest-day"}]
          :measures [{:name "m1"}]
          :viztype :totals
          :pinboard {:measure {:name "m1"} :dimensions []}}
         fields))

(deftest designer->report-tests
  (testing "should mantain the cube name"
    (is (= "wikiticker" (-> {:report {:cube "wikiticker"}} designer designer->report :cube))))

  (testing "should store only the selected measures names"
    (is (= ["added" "deleted"]
           (-> {:measures [{:name "added"} {:name "deleted"}]}
               designer designer->report :measures))))

  (testing "in each filter should store only the dimension name besides its own fields, dates and sets"
    (is (= [{:name "time" :period "current-day"}
            {:name "page" :operator "exclude" :value [nil]}]
           (-> {:filters [{:name "time" :type "..." :period "current-day"}
                          {:name "page" :type "..." :operator "exclude" :value #{nil}}]}
               designer designer->report :filters)))

    (let [from (t/date-time 2018 4 4)
          to (t/date-time 2018 4 5)]
      (is (= [{:interval [(t/to-iso8601 from) (-> to t/end-of-day t/to-iso8601)]}]
             (-> {:filters [{:interval [from to]}]}
                 designer designer->report :filters)))))

  (testing "in each split should store only the dimension name besides its own fields"
    (is (= [{:name "page" :limit 10 :sort-by {:name "page" :descending true}}
            {:name "time" :granularity "P1D" :sort-by {:name "count" :descending false}}]
           (-> {:splits [{:name "page" :type "t" :limit 10
                          :sort-by {:name "page" :type "t" :descending true}}
                         {:name "time" :type "t" :granularity "P1D" :column "..." :extraction "..."
                          :sort-by {:name "count" :type "t" :expression "e" :format "f" :descending false :favorite true}}]}
               designer designer->report :splits))))

  (testing "should convert the pinboard"
    (is (= {:measure "count"
            :dimensions [{:name "time" :granularity "PT1H" :sort-by {:name "time" :descending false}}]}
           (-> {:pinboard {:measure {:name "count" :type "..."}
                           :dimensions [{:name "time" :type "..." :granularity "PT1H"
                                         :sort-by {:name "time" :title "..." :descending false}}]}}
               designer designer->report :pinboard))))

  (testing "should not store user-id as the URL"
    (is (without? :user-id (designer->report (designer {})))))

  (testing "should convert the viztype"
    (is (= "pie-chart" (:viztype (designer->report (designer {:viztype :pie-chart})))))))

(deftest report->designer-tests
  (testing "should convert back to dates and sets and add title and other fields"
    (let [from (t/date-time 2018 4 4)
          to (t/date-time 2018 4 5)]
      (is (= [{:name "time" :title "Fecha" :period "current-day"}
              {:name "time2" :interval [from to]}
              {:name "page" :title "Pag" :operator "exclude" :value #{nil}}]
             (-> {:filters [{:name "time" :period "current-day"}
                            {:name "time2" :interval [(t/to-iso8601 from) (t/to-iso8601 to)]}
                            {:name "page" :operator "exclude" :value [nil]}]}
                 (report->designer {:dimensions [{:name "time" :title "Fecha"}
                                                 {:name "time2"}
                                                 {:name "page" :title "Pag"}]})
                 :filters)))))

  (testing "should converted back the pinboard with the info in the cube"
    (is (= {:measure {:name "count" :type "longSum"}
            :dimensions [{:name "time" :title "Time"}]}
           (-> {:pinboard {:measure "count"
                           :dimensions [{:name "time"}]}}
               (report->designer {:dimensions [{:name "otherD" :title "..."} {:name "time" :title "Time"}]
                                  :measures [{:name "otherM" :type "..."} {:name "count" :type "longSum"}]})
               :pinboard))))

  (testing "if a pinboard dimension no longer exist, it should be removed"
    (is (= []
           (-> {:pinboard {:dimensions [{:name "oldName"}]}}
               (report->designer {:dimensions [{:name "newName" :title "..."}]})
               :pinboard
               :dimensions))))

  (testing "splits and filters must be vectors"
    (is (vector? (:splits (report->designer {} {}))))
    (is (vector? (:filters (report->designer {} {})))))

  (testing "if the pinboard measure in the report doesn't exist on the cube (because the user shouldn't see it) should use the first available measure"
    (is (= {:measure {:name "count" :type "longSum"} :dimensions []}
           (-> {:pinboard {:measure "secretAmount"}}
               (report->designer {:measures [{:name "count" :type "longSum"}]})
               :pinboard))))

  (testing "should convert back the viztype"
    (is (= :pie-chart (:viztype (report->designer {:viztype "pie-chart"} {})))))

  (testing "if a report measure is not present on the cube (because modified permissions) should remove it"
    (is (= [{:name "m2" :title "M2"}]
           (-> {:measures ["m1" "m2"]}
               (report->designer {:measures [{:name "m2" :title "M2"}]})
               :measures)))))
