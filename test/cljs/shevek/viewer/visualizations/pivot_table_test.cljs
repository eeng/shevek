(ns shevek.viewer.visualizations.pivot-table-test
  (:require-macros [cljs.test :refer [deftest testing is use-fixtures]])
  (:require [pjstadig.humane-test-output]
            [shevek.support.reagent :refer [with-container render-component texts]]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]))

(deftest table-visualization-tests
  (testing "one dimension on rows and two measures"
    (render-component
     [table-visualization {:measures [{:name "count" :title "Cantidad"}
                                      {:name "delta" :title "Delta"}]
                           :splits [{:name "country" :title "País"}]
                           :results [{:count 300 :delta 0.5}
                                     {:count 200 :delta 0.2 :country "Canada"}
                                     {:count 100 :delta 0.3 :country "Argentina"}]}])
    (is (= ["País" "Cantidad" "Delta"] (texts ".pivot-table thead th")))
    (is (= [["Total" "300" "0.5"]
            ["Canada" "200" "0.2"]
            ["Argentina" "100" "0.3"]]
           (texts ".pivot-table tbody tr" "td"))))

  (testing "two dimensions on rows"
    (render-component
     [table-visualization {:splits [{:name "country" :title "Country"} {:name "city" :title "City"}]
                           :measures [{:name "count" :title "Count"}]
                           :results [{:count 300}
                                     {:count 200 :country "Canada" :child-rows [{:count 130 :city "Vancouver"}
                                                                                {:count 70 :city "Toronto"}]}
                                     {:count 100 :country "Argentina" :child-rows [{:count 100 :city "Santa Fe"}]}]}])
    (is (= [["Country, City" "Count"]
            ["Total" "300"]
            ["Canada" "200"]
            ["Vancouver" "130"]
            ["Toronto" "70"]
            ["Argentina" "100"]
            ["Santa Fe" "100"]]
           (texts ".pivot-table tr" "th,td"))))

 #_(testing "one dimension on rows, one on columns and one measure"
    (render-component
     [table-visualization {:measures [{:name "count" :title "Count"}]
                           :splits [{:name "country" :title "Country" :on "rows"}
                                    {:name "isRobot" :title "Is Robot" :on "columns"}]
                           :results [{:count 300 :child-rows [{:count 210 :isRobot "Yes"}
                                                              {:count 90 :isRobot "No"}]}
                                     {:count 200 :country "Chile" :child-rows [{:count 130 :isRobot "Yes"}
                                                                               {:count 70 :isRobot "No"}]}
                                     {:count 100 :country "Brasil" :child-rows [{:count 80 :isRobot "Yes"}
                                                                                {:count 20 :isRobot "No"}]}]}])
    (is (= [["Count" "Is Robot"]
            ["Country" "Yes" "No" "Total"]
            ["Total" "210" "90" "300"]
            ["Chile" "130" "70" "200"]
            ["Brasil" "80" "20" "100"]]
           (texts ".pivot-table tr" "th,td"))))

 #_(testing "one dimension on rows, one on columns and two measure"
    (render-component
     [table-visualization {:measures [{:name "m1" :title "M1"}
                                      {:name "m2" :title "M2"}]
                           :splits [{:name "country" :title "Country" :on "rows"}
                                    {:name "isRobot" :title "Is Robot" :on "columns"}]
                           :results [{:m1 300 :m2 30 :child-rows [{:m1 210 :m2 21 :isRobot "Yes"}
                                                                  {:m1 90 :m2 9 :isRobot "No"}]}
                                     {:m1 200 :m2 20 :country "Chile" :child-rows [{:m1 130 :m2 13 :isRobot "Yes"}
                                                                                   {:m1 70 :m2 7 :isRobot "No"}]}
                                     {:m1 100 :m2 10 :country "Brasil" :child-rows [{:m1 80 :m2 8 :isRobot "Yes"}
                                                                                    {:m1 20 :m2 2 :isRobot "No"}]}]}])
    (is (= [["" "Is Robot"]
            ["" "Yes" "No" "Total"]
            ["Country" "M1" "M2" "M1" "M2" "Total M1" "Total M2"]
            ["Total" "210" "21" "90" "9" "300" "30"]
            ["Chile" "130" "13" "70" "7" "200" "20"]
            ["Brasil" "80" "8" "20" "2" "100" "10"]]
           (texts ".pivot-table tr" "th,td"))))

  (testing "measure formatting"
    (render-component
     [table-visualization {:measures [{:name "amount" :format "$0,0.00a"}]
                           :splits [{:name "country"}]
                           :results [{:amount 300}]}])
    (is (= ["Total" "$300.00"] (texts ".pivot-table td"))))

  (testing "dimension formatting"
    (render-component
     [table-visualization {:measures [{:name "amount"}]
                           :splits [{:name "__time" :granularity "P1D"}]
                           :results [{:__time "2017-08-28T10:00:00.000-03:00" :amount 100}
                                     {:__time nil :amount 200}]}])
    (is (= [["Aug 28, 2017" "100"]
            ["Ø" "200"]]
           (texts ".pivot-table tbody tr" "td")))))

(use-fixtures :each with-container)
