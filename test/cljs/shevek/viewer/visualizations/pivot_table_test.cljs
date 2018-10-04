(ns shevek.viewer.visualizations.pivot-table-test
  (:require [cljs.test :refer [deftest testing is use-fixtures]]
            [shevek.support.reagent :refer [with-container render-component texts]]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]))

(deftest table-visualization-tests
  (testing "one dimension on rows, one on columns and one measure"
    (render-component
     [table-visualization {:measures [{:name "m" :title "M"}]
                           :splits [{:name "a" :title "A" :on "rows"}
                                    {:name "b" :title "B" :on "columns"}]
                           :results [{:m 300 :child-cols [{:m 210 :b "B1"} {:m 90 :b "B2"}]}
                                     {:m 200 :a "A1" :child-cols [{:m 130 :b "B1"} {:m 70 :b "B2"}]}
                                     {:m 100 :a "A2" :child-cols [{:m 80 :b "B1"} {:m 20 :b "B2"}]}]}])
    (is (= [["M" "B"]
            ["A" "B1" "B2" "Grand Total"]
            ["Grand Total" "210" "90" "300"]
            ["A1" "130" "70" "200"]
            ["A2" "80" "20" "100"]]
           (texts ".pivot-table tr" "th,td"))))

  (testing "measure formatting"
    (render-component
     [table-visualization {:measures [{:name "amount" :format "$0,0.00a"}]
                           :splits [{:name "country"}]
                           :results [{:amount 300}]}])
    (is (= ["Grand Total" "$300.00"] (texts ".pivot-table td"))))

  (testing "dimension formatting"
    (render-component
     [table-visualization {:measures [{:name "amount"}]
                           :splits [{:name "__time" :granularity "P1D" :empty-value "No Value"}]
                           :results [{:amount 300}
                                     {:__time nil :amount 200}
                                     {:__time "2017-08-28T10:00:00.000-03:00" :amount 100}]}])
    (is (= [["Grand Total" "300"]
            ["No Value" "200"]
            ["Aug 28, 2017" "100"]]
           (texts ".pivot-table tbody tr" "td")))))

(use-fixtures :each with-container)
