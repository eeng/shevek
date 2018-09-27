(ns shevek.domain.exporters.csv-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [shevek.domain.exporters.csv :as csv]
            [goog.labs.format.csv :as gcsv]))

(defn generate-and-parse [viz]
  (-> viz csv/generate gcsv/parse js->clj))

(deftest generate-test
  (testing "totals only"
    (is (= [["" "Added" "Count"]
            ["Total" "20" "10"]]
           (generate-and-parse
            {:measures [{:name "added" :title "Added"}
                        {:name "count" :title "Count"}]
             :results [{:count 10, :added 20}]}))))

  (testing "dimension formatting"
    (is (= [["Is Robot" "Count"]
            ["Total" "10"]
            ["Ø" "8"]
            ["Yes" "2"]]
           (generate-and-parse
            {:splits [{:name "isRobot" :title "Is Robot" :type "BOOL"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 10}
                       {:count 8 :isRobot nil}
                       {:count 2 :isRobot "true"}]}))))

  (testing "one row dimension and one measure"
    (is (= [["Country" "Count"]
            ["Total" "10"]
            ["AR" "8"]
            ["CH" "2"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 10}
                       {:count 8 :country "AR"}
                       {:count 2 :country "CH"}]}))))

  (testing "two row dimensions and one measure"
    (is (= [["Country, City" "Count"]
            ["Total" "10"]
            ["AR" "8"]
            ["     cba" "5"]
            ["     sfe" "3"]
            ["CH" "2"]
            ["     sgo" "2"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country"}
                      {:name "city" :title "City"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 10}
                       {:count 8 :country "AR" :child-rows [{:count 5 :city "cba"} {:count 3 :city "sfe"}]}
                       {:count 2 :country "CH" :child-rows [{:count 2 :city "sgo"}]}]}))))

  (testing "one column dimension and one measure"
    (is (= [["Count" "Country"]
            ["" "AR" "CH" "Total"]
            ["Total" "5" "2" "7"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country" :on "columns"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 7 :child-cols [{:count 5 :country "AR"}
                                              {:count 2 :country "CH"}]}]}))))

  (testing "one row dimension, one column dimension and two measures"
    (is (= [["" "Is New"]
            ["" "No" "" "Yes" "" "Total" ""]
            ["Country" "Count" "Added" "Count" "Added" "Count" "Added"]
            ["Total" "7" "77" "3" "33" "9" "99"]
            ["AR" "6" "66" "" "" "8" "88"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country"}
                      {:name "isNew" :title "Is New" :on "columns"}]
             :measures [{:name "count" :title "Count"}
                        {:name "added" :title "Added"}]
             :results [{:count 9, :added 99
                        :child-cols [{:count 7, :added 77 :isNew "No"}
                                     {:count 3, :added 33 :isNew "Yes"}]}
                       {:count 8, :added 88 :country "AR"
                        :child-cols [{:count 6, :added 66 :isNew "No"}]}]}))))
  (testing "two column dimensions and one measure"
    (is (= [["Count" "Country, Is New"]
            ["" "AR" "" "" "CH" "" "Total"]
            ["" "Yes" "No" "Total" "No" "Total" "Total"]
            ["Total" "1" "4" "5" "2" "2" "7"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country" :on "columns"}
                      {:name "isNew" :title "Is New" :on "columns"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 7
                        :child-cols [{:count 5 :country "AR"
                                      :child-cols [{:isNew "Yes" :count 1} {:isNew "No" :count 4}]}
                                     {:count 2 :country "CH"
                                      :child-cols [{:isNew "No" :count 2}]}]}]}))))

  (testing "two column dimensions and two measures"
    (is (= [["" "Country, Is New"]
            ["" "AR" "" "" "" "" ""         "CH" "" "" ""       "Total" ""]
            ["" "Yes" "" "No" "" "Total" "" "No" "" "Total" "" "Total" ""]
            ["" "M1" "M2" "M1" "M2" "M1" "M2" "M1" "M2" "M1" "M2" "M1" "M2"]
            ["Total" "1" "11" "4" "44" "5" "55" "2" "22" "2" "22" "7" "77"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country" :on "columns"}
                      {:name "isNew" :title "Is New" :on "columns"}]
             :measures [{:name "m1" :title "M1"} {:name "m2" :title "M2"}]
             :results [{:m1 7 :m2 77
                        :child-cols [{:m1 5 :m2 55 :country "AR"
                                      :child-cols [{:isNew "Yes" :m1 1 :m2 11} {:isNew "No" :m1 4 :m2 44}]}
                                     {:m1 2 :m2 22 :country "CH"
                                      :child-cols [{:isNew "No" :m1 2 :m2 22}]}]}]}))))

  (testing "two row dimensions, one column dimension and one measure"
    (is (= [["Count" "Is New"]
            ["Country, City" "No" "Yes" "Total"]
            ["Total" "7" "3" "10"]
            ["AR" "7" "1" "8"]
            ["     cba" "" "1" "5"]
            ["     sfe" "3" "" "3"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country"}
                      {:name "city" :title "City"}
                      {:name "isNew" :title "Is New" :on "columns"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 10
                        :child-cols [{:count 7 :isNew "No"} {:count 3 :isNew "Yes"}]}
                       {:count 8 :country "AR"
                        :child-cols [{:count 7 :isNew "No"} {:count 1 :isNew "Yes"}]
                        :child-rows [{:count 5 :city "cba"
                                      :child-cols [{:count 1 :isNew "Yes"}]}
                                     {:count 3 :city "sfe"
                                      :child-cols [{:count 3 :isNew "No"}]}]}]})))))
