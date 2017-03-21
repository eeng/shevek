(ns pivot.engines.druid-test
  (:require [clojure.test :refer :all]
            [stub-http.core :refer :all]
            [pivot.asserts :refer [submaps? submap?]]
            [pivot.engines.druid :refer [datasources dimensions metrics to-druid-query from-druid-results]]
            [clojure.java.io :as io]))

(defn druid-res [name]
  {:status 200
   :content-type "application/json"
   :body (-> (str "druid_responses/" name ".json") io/resource slurp)})

(deftest datasources-test
  (with-routes!
    {{:method :get :path "/druid/v2/datasources"} (druid-res "datasources")}
    (is (= [{:name "ds1"} {:name "ds2"}] (datasources uri)))))

(deftest dimensions-test
  (with-routes!
    {{:method :post :path "/druid/v2"} (druid-res "segment_metadata")}
    (is (submaps? [{:name "__time"} {:name "cityName"} {:name "countryName"}]
                  (dimensions uri "wikiticker")))))

(deftest metrics-test
  (with-routes!
    {{:method :post :path "/druid/v2"} (druid-res "segment_metadata")}
    (is (submaps? [{:name "added"} {:name "count"}]
                  (metrics uri "wikiticker")))))

(deftest to-druid-query-test
  (testing "totals query"
    (is (submap? {:queryType "timeseries"
                  :dataSource {:type "table" :name "wikiticker"}
                  :granularity {:type "all"}
                  :intervals "2015/2016"
                  :aggregations [{:name "count" :fieldName "count" :type "longSum"}]}
                 (to-druid-query {:cube "wikiticker"
                                  :measures [{:name "count" :type "longSum"}]
                                  :interval ["2015" "2016"]}))))

  (testing "measure type should be doubleSum when empty"
    (is (= "doubleSum"
           (-> (to-druid-query {:measures [{:name "count"}]})
               (get-in [:aggregations 0 :type])))))

  (testing "query with one no-time dimension should generate a topN query"
    (is (submap? {:queryType "topN"
                  :dataSource {:type "table" :name "wikiticker"}
                  :granularity {:type "all"}
                  :dimension "page"
                  :metric "count"
                  :aggregations [{:name "count" :fieldName "count" :type "longSum"}]
                  :threshold 10}
                 (to-druid-query {:cube "wikiticker"
                                  :dimension {:name "page" :limit 10}
                                  :measures [{:name "count" :type "longSum"}]}))))

  (testing "query with one time dimension should generate a timeseries query"
    (is (submap? {:queryType "timeseries"
                  :dataSource {:type "table" :name "wikiticker"}
                  :intervals "2015/2016"
                  :aggregations [{:name "count" :fieldName "count" :type "longSum"}]
                  :granularity {:type "period"
                                :period "P1D"}
                  :descending true}
                 (to-druid-query {:cube "wikiticker"
                                  :measures [{:name "count" :type "longSum"}]
                                  :interval ["2015" "2016"]
                                  :dimension {:name "__time" :granularity "P1D" :descending true}})))))

(deftest from-druid-results-test
  (testing "topN results"
    (is (submaps? [{:page "P1" :count 1}
                   {:page "P2" :count 2}]
                  (from-druid-results
                    {:dimension {:name "page"}}
                    {:queryType "topN"}
                    [{:result [{:page "P1" :count 1}
                               {:page "P2" :count 2}]}]))))

  (testing "timeseries results"
    (is (submaps? [{:__time "2015" :count 1}
                   {:__time "2016" :count 2}]
                  (from-druid-results
                   {:dimension {:name "__time"}}
                   {:queryType "timeseries"}
                   [{:result {:count 1} :timestamp "2015"}
                    {:result {:count 2} :timestamp "2016"}])))))
