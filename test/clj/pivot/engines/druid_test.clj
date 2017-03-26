(ns pivot.engines.druid-test
  (:require [clojure.test :refer :all]
            [stub-http.core :refer :all]
            [pivot.asserts :refer [submaps? submap? without?]]
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

  (testing "query with one atemporal dimension should generate a topN query"
    (is (submap? {:queryType "topN"
                  :dataSource {:type "table" :name "wikiticker"}
                  :granularity {:type "all"}
                  :dimension "page"
                  :metric {:type "numeric" :metric "count"}
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
                                  :dimension {:name "__time" :granularity "P1D" :sort-by {:descending true}}}))))

  (testing "filter"
    (testing ":is operator"
      (is (submap? {:filter {:type "selector"
                             :dimension "isRobot"
                             :value "true"}}
                   (to-druid-query {:filter [{:name "__time"} {:name "isRobot" :operator "is" :value "true"}]})))
      (is (submap? {:filter {:type "selector"
                             :dimension "countryName"
                             :value nil}}
                   (to-druid-query {:filter [ {:name "countryName" :operator "is" :value nil}]}))))

    (testing "two filters should generate an 'and' filter"
      (is (submap? {:filter {:type "and"
                             :fields [{:type "selector" :dimension "isRobot" :value "true"}
                                      {:type "selector" :dimension "isNew" :value "false"}]}}
                   (to-druid-query {:measures [{:name "count" :type "longSum"}]
                                    :filter [{:name "__time"} ; Este se ignora xq se manda en el interval
                                             {:name "isRobot" :operator "is" :value "true"}
                                             {:name "isNew" :operator "is" :value "false"}]})))
      (is (without? :filter (to-druid-query {:filter [{:name "isRobot"} {:name "isNew"}]})))
      (is (without? :filter (to-druid-query {:filter [{:name "isRobot" :operator "include" :value []}]}))))

    (testing ":include operator"
      (is (submap? {:filter {:type "in"
                             :dimension "countryName"
                             :values ["Italy" "France"]}}
                   (to-druid-query {:filter [{:name "countryName" :operator "include" :value ["Italy" "France"]}]}))))

    (testing ":exclude operator"
      (is (submap? {:filter {:type "not"
                             :field {:type "in"
                                     :dimension "countryName"
                                     :values [nil "France"]}}}
                   (to-druid-query {:filter [{:name "countryName" :operator "exclude" :value [nil "France"]}]}))))

    (testing ":search operator"
      (is (submap? {:filter {:type "search"
                             :dimension "countryName"
                             :query {:type "insensitive_contains"
                                     :value "arg"}}}
                   (to-druid-query {:filter [{:name "countryName" :operator "search" :value "arg"}]})))))

  (testing "sorting"
    (testing "query with one atemporal dimension sorting by other selected metric in ascending order"
      (is (submap? {:queryType "topN"
                    :metric {:type "inverted" :metric {:type "numeric" :metric "added"}}}
                   (to-druid-query {:cube "wikiticker"
                                    :dimension {:name "page" :sort-by {:name "added" :descending false}}
                                    :measures [{:name "count" :type "longSum"}
                                               {:name "added" :type "longSum"}]}))))

    (testing "query with one atemporal dimension sorting by other non selected metric should add it to the aggregations"
      (is (submap? {:metric {:type "numeric" :metric "users"}
                    :aggregations [{:name "count" :fieldName "count" :type "longSum"}
                                   {:name "users" :fieldName "users" :type "hyperUnique"}]}
                   (to-druid-query {:dimension {:name "page" :sort-by {:name "users" :type "hyperUnique"}}
                                    :measures [{:name "count" :type "longSum"}]}))))

    (testing "ascending ordered by the same dimension should use lexicographic sorting"
      (is (submap? {:metric {:type "dimension" :ordering "lexicographic"}
                    :aggregations [{:name "count" :fieldName "count" :type "longSum"}]}
                   (to-druid-query {:dimension {:name "page" :sort-by {:name "page" :descending false}}
                                    :measures [{:name "count" :type "longSum"}]}))))

    (testing "descending ordered by the same dimension should use lexicographic sorting"
      (is (submap? {:metric {:type "inverted"
                             :metric {:type "dimension" :ordering "lexicographic"}}}
                   (to-druid-query {:dimension {:name "page" :sort-by {:name "page" :descending true}}}))))))

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
