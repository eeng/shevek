(ns shevek.querying.aggregation-test
  (:require [clojure.test :refer :all]
            [shevek.asserts :refer [submaps?]]
            [shevek.querying.aggregation :refer [query]]
            [shevek.lib.druid-driver :as druid]))

(def dw :dw)

(deftest query-test
  (testing "query with one dimension with totals should issue two druid queries"
    (let [queries-sent (atom [])]
      (with-redefs [druid/send-query (fn [_ dq] (swap! queries-sent conj dq) [])]
        (query dw
               {:cube "wikiticker"
                :split [{:name "page"}]
                :measures [{:name "count" :expression "(sum $count)"}]
                :filter [{:interval ["2015-09-12" "2015-09-13"]}]
                :totals true})
        (is (submaps? [{:queryType "timeseries" :granularity "all"}
                       {:queryType "topN" :dimension "page"}]
                      @queries-sent)))))

  (testing "query with two normal dimensions should issue one query for the first dim and one for each result as filter for the second dim"
    (let [queries-sent (atom [])]
      (with-redefs
        [druid/send-query
         (fn [_ dq]
           (swap! queries-sent conj dq)
           (cond
             (= "country" (dq :dimension))
             [{:result [{:country "Argentina" :count 1} {:country "Brasil" :count 2}]}]
             (= "Argentina" (get-in dq [:filter :value]))
             [{:result [{:city "Cordoba" :count 3} {:city "Rafaela" :count 4}]}]
             (= "Brasil" (get-in dq [:filter :value]))
             [{:result [{:city "Rio de Janerio" :count 5}]}]))]
        (is (submaps? [{:country "Argentina" :count 1 :_results [{:city "Cordoba" :count 3} {:city "Rafaela" :count 4}]}
                       {:country "Brasil" :count 2 :_results [{:city "Rio de Janerio" :count 5}]}]
                      (query dw
                             {:cube "wikiticker"
                              :split [{:name "country"} {:name "city"}]
                              :measures [{:name "count" :expression "(sum $count)"}]
                              :filter [{:interval ["2015-09-12" "2015-09-13"]}]})))
        (is (submaps? [{:queryType "topN" :dimension "country"}
                       {:queryType "topN" :dimension "city"
                        :filter {:dimension "country" :type "selector" :value "Argentina"}}
                       {:queryType "topN" :dimension "city"
                        :filter {:dimension "country" :type "selector" :value "Brasil"}}]
                      (sort-by (juxt (comp not nil? :filter) (comp :value :filter)) @queries-sent))))))

  (testing "query with one time and one normal dimension should issue one query for the time dim and for each result should issue another query with the interval set accordingly"
    (let [queries-sent (atom [])]
      (with-redefs
        [druid/send-query
         (fn [_ dq]
           (swap! queries-sent conj dq)
           (cond
             (= "timeseries" (dq :queryType))
             [{:result {:count 1} :timestamp "2015-09-01T00:00:00.000Z"}
              {:result {:count 2} :timestamp "2015-09-01T12:00:00.000Z"}]
             :else []))]
        (query dw
               {:cube "wikiticker"
                :split [{:name "__time" :granularity "PT12H"} {:name "country"}]
                :measures [{:name "count" :expression "(sum $count)"}]
                :filter [{:interval ["2015-09-01" "2015-09-01"]}
                         {:name "country" :operator "is" :value "Argentina"}]})
        (is (submaps? [{:queryType "timeseries"}
                       {:queryType "topN" :dimension "country"
                        :intervals "2015-09-01T00:00:00.000Z/2015-09-01T12:00:00.000Z"
                        :filter {:dimension "country" :type "selector" :value "Argentina"}}
                       {:queryType "topN" :dimension "country"
                        :intervals "2015-09-01T12:00:00.000Z/2015-09-02T00:00:00.000Z"
                        :filter {:dimension "country" :type "selector" :value "Argentina"}}]
                      (sort-by :intervals @queries-sent)))))))
