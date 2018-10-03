(ns shevek.domain.exporters.csv-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [shevek.domain.exporters.csv :as csv]
            [goog.labs.format.csv :as gcsv]))

(defn generate-and-parse [viz]
  (-> viz csv/generate gcsv/parse js->clj))

(deftest generate-test
  (testing "totals only"
    (is (= [["" "Added" "Count"]
            ["Grand Total" "20" "10"]]
           (generate-and-parse
            {:measures [{:name "added" :title "Added"}
                        {:name "count" :title "Count"}]
             :results [{:count 10, :added 20}]}))))

  (testing "dimension formatting"
    (is (= [["Is Robot" "Count"]
            ["Grand Total" "10"]
            ["Ø" "8"]
            ["Yes" "2"]]
           (generate-and-parse
            {:splits [{:name "isRobot" :title "Is Robot" :type "BOOL"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 10}
                       {:count 8 :isRobot nil}
                       {:count 2 :isRobot "true"}]}))))

  (testing "two row dimensions and one measure"
    (is (= [["Country, City" "Count"]
            ["Grand Total" "10"]
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

  (testing "one row dimension, one column dimension and two measures"
    (is (= [["" "Is New"]
            ["" "No" "" "Yes" "" "Total" ""]
            ["Country" "Count" "Added" "Count" "Added" "Count" "Added"]
            ["Grand Total" "7" "77" "3" "33" "9" "99"]
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
                        :child-cols [{:count 6, :added 66 :isNew "No"}]}]})))))
