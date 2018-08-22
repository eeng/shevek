(ns shevek.viewer.visualizations.pivot-table-test
  (:require [cljs.test :refer [deftest testing is use-fixtures]]
            [shevek.support.reagent :refer [with-container render-component texts]]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]))

(deftest table-visualization-tests
  (testing "one dimension on rows and two measures"
    (render-component
     [table-visualization {:measures [{:name "m1" :title "M1"}
                                      {:name "m2" :title "M2"}]
                           :splits [{:name "a" :title "A"}]
                           :results [{:m1 300 :m2 0.5}
                                     {:m1 200 :m2 0.2 :a "A1"}
                                     {:m1 100 :m2 0.3 :a "A2"}]}])
    (is (= ["A" "M1" "M2"] (texts ".pivot-table thead th")))
    (is (= [["Total" "300" "0.5"]
            ["A1" "200" "0.2"]
            ["A2" "100" "0.3"]]
           (texts ".pivot-table tbody tr" "td"))))

  (testing "two dimensions on rows"
    (render-component
     [table-visualization {:splits [{:name "a" :title "A"} {:name "b" :title "B"}]
                           :measures [{:name "m" :title "M"}]
                           :results [{:m 300}
                                     {:m 200 :a "A1" :child-rows [{:m 130 :b "B11"} {:m 70 :b "B12"}]}
                                     {:m 100 :a "A2" :child-rows [{:m 100 :b "B21"}]}]}])
    (is (= [["A, B" "M"]
            ["Total" "300"]
            ["A1" "200"]
            ["B11" "130"]
            ["B12" "70"]
            ["A2" "100"]
            ["B21" "100"]]
           (texts ".pivot-table tr" "th,td")))

   (testing "one dimension on columns"
     (render-component
      [table-visualization {:measures [{:name "m" :title "M"}]
                            :splits [{:name "a" :title "A" :on "columns"}]
                            :results [{:m 100 :child-cols [{:m 60 :a "A1"} {:m 40 :a "A2"}]}]}])
     (is (= [["M"     "A"]
             [""      "A1" "A2" "Total"]
             ["Total" "60" "40" "100"]]
            (texts ".pivot-table tr" "th,td")))))

  (testing "two dimension on columns and one measure"
     (render-component
      [table-visualization {:measures [{:name "m" :title "M"}]
                            :splits [{:name "a" :title "A" :on "columns"}
                                     {:name "b" :title "B" :on "columns"}]
                            :results [{:m 100
                                       :child-cols [{:m 60 :a "A1" :child-cols [{:m 20 :b "B1"} {:m 40 :b "B2"}]}
                                                    {:m 40 :a "A2" :child-cols [{:m 15 :b "B1"} {:m 25 :b "B2"}]}]}]}])
     (is (= [["M"     "A"]
             [""      "A1"              "A2"              "Total"]
             [""      "B"]
             [""      "B1" "B2" "Total" "B1" "B2" "Total" "Total"]
             ["Total" "20" "40" "60"    "15" "25" "40"    "100"]]
            (texts ".pivot-table tr" "th,td"))))

  (testing "one dimension on rows, one on columns and one measure"
    (render-component
     [table-visualization {:measures [{:name "m" :title "M"}]
                           :splits [{:name "a" :title "A" :on "rows"}
                                    {:name "b" :title "B" :on "columns"}]
                           :results [{:m 300 :child-cols [{:m 210 :b "B1"} {:m 90 :b "B2"}]}
                                     {:m 200 :a "A1" :child-cols [{:m 130 :b "B1"} {:m 70 :b "B2"}]}
                                     {:m 100 :a "A2" :child-cols [{:m 80 :b "B1"} {:m 20 :b "B2"}]}]}])
    (is (= [["M" "B"]
            ["A" "B1" "B2" "Total"]
            ["Total" "210" "90" "300"]
            ["A1" "130" "70" "200"]
            ["A2" "80" "20" "100"]]
           (texts ".pivot-table tr" "th,td"))))

  (testing "one dimension on rows, one on columns and two measure"
    (render-component
     [table-visualization {:measures [{:name "m1" :title "M1"}
                                      {:name "m2" :title "M2"}]
                           :splits [{:name "a" :title "A" :on "rows"}
                                    {:name "b" :title "B" :on "columns"}]
                           :results [{:m1 300 :m2 30 :child-cols [{:m1 210 :m2 21 :b "B1"}
                                                                  {:m1 90 :m2 9 :b "B2"}]}
                                     {:m1 200 :m2 20 :a "A1" :child-cols [{:m1 130 :m2 13 :b "B1"}
                                                                          {:m1 70 :m2 7 :b "B2"}]}
                                     {:m1 100 :m2 10 :a "A2" :child-cols [{:m1 80 :m2 8 :b "B1"}
                                                                          {:m1 20 :m2 2 :b "B2"}]}]}])
    (is (= [["B" "B1" "B2" "Total"]
            ["A" "M1" "M2" "M1" "M2" "M1" "M2"]
            ["Total" "210" "21" "90" "9" "300" "30"]
            ["A1" "130" "13" "70" "7" "200" "20"]
            ["A2" "80" "8" "20" "2" "100" "10"]]
           (texts ".pivot-table tr" "th,td"))))

  (testing "missing and unsorted children"
    (render-component
     [table-visualization
      {:measures [{:name "m" :title "M"}]
       :splits [{:name "a" :title "A" :on "rows"}
                {:name "b" :title "B" :on "columns"}
                {:name "c" :title "C" :on "columns"}]
       :results [{:m 300 :child-cols [{:m 100 :b "B1" :child-cols [{:m 30 :c "C1"} {:m 70 :c "C2"}]}
                                      {:m 200 :b "B2" :child-cols [{:m 20 :c "C1"} {:m 180 :c "C2"}]}]}
                 {:m 140 :a "A1" :child-cols [{:m 90 :b "B1" :child-cols [{:m 90 :c "C1"}]}
                                              {:m 50 :b "B2" :child-cols [{:m 50 :c "C2"}]}]}
                 {:m 160 :a "A2" :child-cols [{:m 160 :b "B1" :child-cols [{:m 50 :c "C2"} {:m 110 :c "C1"}]}]}]}])
    (is (= [["Total" "30" "70" "100" "20" "180" "200" "300"]
            ["A1" "90" "" "90" "" "50" "50" "140"]
            ["A2" "110" "50" "160" "" "" "" "160"]]
           (texts ".pivot-table tbody tr" "td"))))

  (testing "measure formatting"
    (render-component
     [table-visualization {:measures [{:name "amount" :format "$0,0.00a"}]
                           :splits [{:name "country"}]
                           :results [{:amount 300}]}])
    (is (= ["Total" "$300.00"] (texts ".pivot-table td"))))

  (testing "dimension formatting"
    (render-component
     [table-visualization {:measures [{:name "amount"}]
                           :splits [{:name "__time" :granularity "P1D" :empty-value "No Value"}]
                           :results [{:__time "2017-08-28T10:00:00.000-03:00" :amount 100}
                                     {:__time nil :amount 200}]}])
    (is (= [["Aug 28, 2017" "100"]
            ["No Value" "200"]]
           (texts ".pivot-table tbody tr" "td")))))

(use-fixtures :each with-container)
