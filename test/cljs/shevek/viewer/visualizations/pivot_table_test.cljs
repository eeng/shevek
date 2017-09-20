(ns shevek.viewer.visualizations.pivot-table-test
  (:require-macros [cljs.test :refer [deftest testing is use-fixtures]])
  (:require [pjstadig.humane-test-output]
            [shevek.asserts :refer [submap? submaps?]]
            [cljs-react-test.utils :as tu]
            [cljs-react-test.simulate :as sim]
            [reagent.core :as r]
            [cljsjs.jquery]
            [shevek.viewer.visualizations.pivot-table :refer [table-visualization]]))

(def ^:dynamic container)

(use-fixtures :each (fn [test-fn]
                      (binding [container (tu/new-container!)]
                        (test-fn)
                        (tu/unmount! container))))

(defn texts
  ([selector]
   (->> selector js/$ .toArray (map #(.-textContent %))))
  ([parent-selector child-selector]
   (->> parent-selector js/$ .toArray
        (map (fn [parent]
               (->> (js/$ child-selector parent) .toArray
                    (map #(.-textContent %))))))))

(deftest table-visualization-tests
  (testing "one dimension and one measure"
    (r/render-component
     [table-visualization {:measures [{:name "count" :title "Cantidad"}]
                           :split [{:name "country" :title "País"}]
                           :results [{:count 300}
                                     {:count 200 :country "Canada"}
                                     {:count 100 :country "Argentina"}]}]
     container)
    (is (= ["País" "Cantidad"] (texts ".pivot-table thead th")))
    (is (= [["Total" "300"]
            ["Canada" "200"]
            ["Argentina" "100"]]
           (texts ".pivot-table tbody tr" "td"))))

  (testing "two dimensions"
    (r/render-component
     [table-visualization {:split [{:name "country" :title "Country"} {:name "city" :title "City"}]
                           :measures [{:name "count" :title "Count"}]
                           :results [{:count 300}
                                     {:count 200 :country "Canada" :_results [{:count 130 :city "Vancouver"}
                                                                              {:count 70 :city "Toronto"}]}
                                     {:count 100 :country "Argentina" :_results [{:count 100 :city "Santa Fe"}]}]}]
     container)
    (is (= [["Country, City" "Count"]
            ["Total" "300"]
            ["Canada" "200"]
            ["Vancouver" "130"]
            ["Toronto" "70"]
            ["Argentina" "100"]
            ["Santa Fe" "100"]]
           (texts ".pivot-table tr" "th,td"))))

  (testing "measure formatting"
    (r/render-component
     [table-visualization {:measures [{:name "amount" :format "$0,0.00a"}]
                           :split [{:name "country"}]
                           :results [{:amount 300}]}]
     container)
    (is (= ["Total" "$300.00"] (texts ".pivot-table td")))))
