(ns shevek.viewer.visualizations.pivot-table-test
  (:require-macros [cljs.test :refer [deftest testing is use-fixtures]])
  (:require [pjstadig.humane-test-output]
            [shevek.support.reagent :refer [with-container render-component texts]]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]))

(deftest table-visualization-tests
  (testing "one dimension and two measures"
    (render-component
     [table-visualization {:measures [{:name "count" :title "Cantidad"}
                                      {:name "delta" :title "Delta"}]
                           :row-splits [{:name "country" :title "País"}]
                           :results [{:count 300 :delta 0.5}
                                     {:count 200 :delta 0.2 :country "Canada"}
                                     {:count 100 :delta 0.3 :country "Argentina"}]}])
    (is (= ["País" "Cantidad" "Delta"] (texts ".pivot-table thead th")))
    (is (= [["Total" "300" "0.5"]
            ["Canada" "200" "0.2"]
            ["Argentina" "100" "0.3"]]
           (texts ".pivot-table tbody tr" "td"))))

  (testing "two dimensions"
    (render-component
     [table-visualization {:row-splits [{:name "country" :title "Country"} {:name "city" :title "City"}]
                           :measures [{:name "count" :title "Count"}]
                           :results [{:count 300}
                                     {:count 200 :country "Canada" :_results [{:count 130 :city "Vancouver"}
                                                                              {:count 70 :city "Toronto"}]}
                                     {:count 100 :country "Argentina" :_results [{:count 100 :city "Santa Fe"}]}]}])
    (is (= [["Country, City" "Count"]
            ["Total" "300"]
            ["Canada" "200"]
            ["Vancouver" "130"]
            ["Toronto" "70"]
            ["Argentina" "100"]
            ["Santa Fe" "100"]]
           (texts ".pivot-table tr" "th,td"))))

  (testing "measure formatting"
    (render-component
     [table-visualization {:measures [{:name "amount" :format "$0,0.00a"}]
                           :row-splits [{:name "country"}]
                           :results [{:amount 300}]}])
    (is (= ["Total" "$300.00"] (texts ".pivot-table td"))))

  (testing "dimension formatting"
    (render-component
     [table-visualization {:measures [{:name "amount"}]
                           :row-splits [{:name "__time" :granularity "P1D"}]
                           :results [{:__time "2017-08-28T10:00:00.000-03:00" :amount 100}
                                     {:__time nil :amount 200}]}])
    (is (= [["Aug 28, 2017" "100"]
            ["Ø" "200"]]
           (texts ".pivot-table tbody tr" "td")))))

(use-fixtures :each with-container)
