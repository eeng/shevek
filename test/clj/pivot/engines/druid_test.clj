(ns pivot.engines.druid-test
  (:require [clojure.test :refer :all]
            [stub-http.core :refer :all]
            [pivot.asserts :refer [submaps? submap?]]
            [pivot.engines.druid :refer [datasources dimensions metrics to-druid-query]]
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

  (testing "measure type should be doubleSum when empty")

  (testing "query with one time dimension and one measure"
    #_(is (submap? {:queryType "timeseries"
                    :dataSource {:type "table" :name "wikiticker"}
                    :intervals "2015/2016"
                    :aggregations [{:name "count" :fieldName "count" :type "longSum"}]
                    :granularity {:type "period"
                                  :period "P1D"
                                  :typeZone "Etc/UTC"}}
                 (to-druid-query {:cube "wikiticker"
                                  :measures [{:name "count" :type "longSum"}]
                                  :interval ["2015" "2016"]
                                  :split [{:name "__time"}]
                                  :granularity {:period "P1D"
                                                :time-zone "Etc/UTC"}})))))
