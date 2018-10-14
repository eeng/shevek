(ns shevek.engine.druid-native.planner-test
  (:require [clojure.test :refer :all]
            [clj-fakes.core :as f]
            [shevek.engine.druid-native.planner :refer [execute-query]]
            [shevek.driver.druid :refer [DruidDriver send-query]]
            [shevek.asserts :refer [submap-arg submaps?]]))

(defn without-concurrency! []
  (f/patch! #'pmap (f/fake [f/any map])))

(defn filter-values [filter]
  (->> filter :fields (map #(or (:value %) (:values %)))))

(defn druid-query? [expected-query-type expected-dimension expected-filter-values
                    {:keys [queryType dimension filter]}]
  (and (= queryType expected-query-type)
       (= dimension expected-dimension)
       (if (vector? expected-filter-values)
         (= (filter-values filter) expected-filter-values)
         (= (filter :value) expected-filter-values))))

(def cube-metadata
  {:measures [{:name "count" :expression "(count)"} {:name "added" :expression "(count)"}]})

(deftest execute-query-test
  (testing "only totals should return row with zeros as Druid return an empty list"
    (f/with-fakes
      (let [driver (f/reify-fake DruidDriver
                     (send-query :fake [[(submap-arg {:queryType "timeseries"})] []]))]
        (is (= [{:count 0 :added 0}]
               (execute-query
                driver
                {:cube "wikiticker"
                 :splits []
                 :measures ["count" "added"]
                 :filters [{:interval ["2015" "2016"]}]
                 :totals true}
                cube-metadata))))))

  (testing "totals and one row split"
    (f/with-fakes
      (let [driver (f/reify-fake DruidDriver
                     (send-query :recorded-fake [f/any []]))]
        (execute-query
         driver
         {:cube "wikiticker"
          :splits [{:name "page"}]
          :measures ["count"]
          :filters [{:interval ["2015" "2016"]}]
          :totals true}
         cube-metadata)
        (f/methods-were-called-in-order
         send-query driver [(submap-arg {:queryType "timeseries" :granularity "all"})]
         send-query driver [(submap-arg {:queryType "topN" :dimension "page"})]))))

  (testing "two row splits"
    (f/with-fakes
      (let [driver (f/reify-fake DruidDriver
                     (send-query :fake
                                 [[(submap-arg {:queryType "topN" :dimension "country"})]
                                  [{:result [{:country "Argentina" :count 1} {:country "Brasil" :count 2}]}]

                                  [(submap-arg {:queryType "topN" :dimension "city"
                                                :filter {:dimension "country" :type "selector" :value "Argentina"}})]
                                  [{:result [{:city "Cordoba" :count 3} {:city "Rafaela" :count 4}]}]

                                  [(submap-arg {:queryType "topN" :dimension "city"
                                                :filter {:dimension "country" :type "selector" :value "Brasil"}})]
                                  [{:result [{:city "Rio de Janerio" :count 5}]}]]))]
        (is (submaps?
             [{:country "Argentina" :count 1 :child-rows [{:city "Cordoba" :count 3}
                                                          {:city "Rafaela" :count 4}]}
              {:country "Brasil" :count 2 :child-rows [{:city "Rio de Janerio" :count 5}]}]
             (execute-query
              driver
              {:cube "wikiticker"
               :splits [{:name "country"} {:name "city"}]
               :measures ["count"]
               :filters [{:interval ["2015" "2016"]}]}
              cube-metadata))))))

  (testing "one time and one normal dimension on rows"
    (f/with-fakes
      (without-concurrency!)
      (let [driver (f/reify-fake DruidDriver
                     (send-query :recorded-fake

                                 [[(submap-arg {:queryType "timeseries"})]
                                  [{:result {:count 1} :timestamp "2015-09-01T00:00:00.000Z"}
                                   {:result {:count 2} :timestamp "2015-09-01T12:00:00.000Z"}]

                                  [f/any] []]))]
        (is (= [{:__time "2015-09-01T00:00:00.000Z" :count 1}
                {:__time "2015-09-01T12:00:00.000Z" :count 2}]
               (execute-query
                driver
                {:cube "wikiticker"
                 :splits [{:name "__time" :granularity "PT12H"} {:name "country"}]
                 :measures ["count"]
                 :filters [{:interval ["2015" "2015"]}
                           {:name "country" :operator "is" :value "Argentina"}]}
                cube-metadata)))
        (f/methods-were-called-in-order
         send-query driver [(submap-arg {:queryType "timeseries"})]
         send-query driver [(submap-arg {:queryType "topN" :dimension "country"
                                         :intervals "2015-09-01T00:00:00.000Z/2015-09-01T12:00:00.000Z"
                                         :filter {:dimension "country" :type "selector" :value "Argentina"}})]
         send-query driver [(submap-arg {:queryType "topN" :dimension "country"
                                         :intervals "2015-09-01T12:00:00.000Z/2015-09-02T00:00:00.000Z"
                                         :filter {:dimension "country" :type "selector" :value "Argentina"}})]))))

  (testing "one column split and one measure"
    (f/with-fakes
      (let [driver (f/reify-fake DruidDriver
                     (send-query :fake
                                 [[(submap-arg {:queryType "timeseries"})]
                                  [{:result {:count 7}}]
                                  [(f/arg (partial druid-query? "topN" "dimA" []))]
                                  [{:result [{:dimA "A1" :count 3} {:dimA "A2" :count 4}]}]]))]
        (is (= [{:count 7 :child-cols [{:dimA "A1" :count 3}
                                       {:dimA "A2" :count 4}]}]
               (execute-query
                driver
                {:cube "wikiticker"
                 :splits [{:name "dimA" :on "columns"}]
                 :measures ["count"]
                 :filters [{:interval ["2015" "2016"]}]
                 :totals true}
                cube-metadata))))))

  (testing "one row split, one column split and one measure"
    (f/with-fakes
      (without-concurrency!)
      (let [driver (f/reify-fake DruidDriver
                     (send-query :fake

                                 [[(submap-arg {:queryType "timeseries"})]
                                  [{:result {:count 100}}]

                                  [(f/arg (partial druid-query? "topN" "dimB" []))]
                                  [{:result [{:dimB "B1" :count 35} {:dimB "B2" :count 65}]}]

                                  [(f/arg (partial druid-query? "topN" "dimA" []))]
                                  [{:result [{:dimA "A1" :count 60} {:dimA "A2" :count 40}]}]

                                  [(f/arg (partial druid-query? "topN" "dimB" ["A1" ["B1" "B2"]]))]
                                  [{:result [{:dimB "B2" :count 40} {:dimB "B1" :count 20}]}] ; Sorted different than the totals child-cols

                                  [(f/arg (partial druid-query? "topN" "dimB" ["A2" ["B1" "B2"]]))]
                                  [{:result [{:dimB "B2" :count 25}]}]]))] ; Missing B1 value child-cols

        (is (= [{:count 100
                 :child-cols [{:dimB "B1" :count 35} {:dimB "B2" :count 65}]}
                {:count 60 :dimA "A1"
                 :child-cols [{:dimB "B2" :count 40} {:dimB "B1" :count 20}]}
                {:count 40 :dimA "A2"
                 :child-cols [{:dimB "B2" :count 25}]}]
               (execute-query
                driver
                {:cube "wikiticker"
                 :splits [{:name "dimA" :on "rows"} {:name "dimB" :on "columns"}]
                 :measures ["count"]
                 :filters [{:interval ["2015" "2016"]}]
                 :totals true}
                cube-metadata))))))

  (testing "one row split, one column temporal split and one measure"
    (f/with-fakes
      (without-concurrency!)
      (let [driver (f/reify-fake DruidDriver
                     (send-query :fake

                                 [[(submap-arg {:queryType "timeseries" :granularity "all"})]
                                  [{:result {:count 100}}]

                                  [(f/arg (fn [{:keys [queryType filter granularity] :as dq}]
                                            (and (= queryType "timeseries") (= (:period granularity) "P1D") (nil? filter))))]
                                  [{:result {:count 35} :timestamp "2001"} {:result {:count 65} :timestamp "2002"}]

                                  [(f/arg (fn [{:keys [queryType dimension filter] :as dq}]
                                            (and (= queryType "topN") (= dimension "dimA") (nil? filter))))]
                                  [{:result [{:dimA "A1" :count 60}]}]

                                  [(f/arg (fn [{:keys [queryType filter granularity] :as dq}]
                                            (and (= queryType "timeseries") (= (:period granularity) "P1D") (= (filter :value) "A1"))))]
                                  [{:result {:count 40} :timestamp "2001"} {:result {:count 20} :timestamp "2002"}]]))]

        (is (= [{:count 100
                 :child-cols [{:dimB "2001" :count 35} {:dimB "2002" :count 65}]}
                {:count 60 :dimA "A1"
                 :child-cols [{:dimB "2001" :count 40} {:dimB "2002" :count 20}]}]
               (execute-query
                driver
                {:cube "wikiticker"
                 :splits [{:name "dimA" :on "rows"} {:name "dimB" :on "columns" :granularity "P1D"}]
                 :measures ["count"]
                 :filters [{:interval ["2001" "2002"]}]
                 :totals true}
                cube-metadata))))))

  (testing "two column splits and one measure"
    (f/with-fakes
      (without-concurrency!)
      (let [driver (f/reify-fake DruidDriver
                     (send-query :fake

                                 [[(submap-arg {:queryType "timeseries"})]
                                  [{:result {:count 100}}]

                                  [(f/arg (partial druid-query? "topN" "dimA" []))]
                                  [{:result [{:dimA "A1" :count 60} {:dimA "A2" :count 40}]}]

                                  [(f/arg (partial druid-query? "topN" "dimB" "A1"))]
                                  [{:result [{:dimB "B1" :count 20} {:dimB "B2" :count 40}]}]

                                  [(f/arg (partial druid-query? "topN" "dimB" "A2"))]
                                  [{:result [{:dimB "B1" :count 15} {:dimB "B2" :count 25}]}]]))]

        (is (= [{:count 100
                 :child-cols [{:dimA "A1" :count 60
                               :child-cols [{:dimB "B1" :count 20} {:dimB "B2" :count 40}]}
                              {:dimA "A2" :count 40
                               :child-cols [{:dimB "B1" :count 15} {:dimB "B2" :count 25}]}]}]
               (execute-query
                driver
                {:cube "wikiticker"
                 :splits [{:name "dimA" :on "columns"} {:name "dimB" :on "columns"}]
                 :measures ["count"]
                 :filters [{:interval ["2015" "2016"]}]
                 :totals true}
                cube-metadata))))))

  (testing "two row splits, one column split and one measure"
    (f/with-fakes
      (without-concurrency!)
      (let [driver (f/reify-fake DruidDriver
                     (send-query :fake

                                 [[(submap-arg {:queryType "timeseries"})]
                                  [{:result {:count 100}}]

                                  [(f/arg (partial druid-query? "topN" "dimC" []))]
                                  [{:result [{:dimC "C1" :count 35} {:dimC "C2" :count 65}]}]

                                  [(f/arg (partial druid-query? "topN" "dimA" []))]
                                  [{:result [{:dimA "A1" :count 60} {:dimA "A2" :count 40}]}]

                                  [(f/arg (partial druid-query? "topN" "dimB" "A1"))]
                                  [{:result [{:dimB "B1" :count 10} {:dimB "B2" :count 50}]}]

                                  [(f/arg (partial druid-query? "topN" "dimC" ["A1" "B1" ["C1" "C2"]]))]
                                  [{:result [{:dimC "C1" :count 5} {:dimC "C2" :count 5}]}]

                                  [(f/arg (partial druid-query? "topN" "dimC" ["A1" "B2" ["C1" "C2"]]))]
                                  [{:result [{:dimC "C1" :count 15} {:dimC "C2" :count 35}]}]

                                  [(f/arg (partial druid-query? "topN" "dimC" ["A1" ["C1" "C2"]]))]
                                  [{:result [{:dimC "C1" :count 20} {:dimC "C2" :count 40}]}]

                                  [(f/arg (partial druid-query? "topN" "dimB" "A2"))]
                                  [{:result []}]

                                  [(f/arg (partial druid-query? "topN" "dimC" ["A2" ["C1" "C2"]]))]
                                  [{:result [{:dimC "C1" :count 15} {:dimC "C2" :count 25}]}]]))]


        (is (= [{:count 100
                 :child-cols [{:dimC "C1" :count 35} {:dimC "C2" :count 65}]}
                {:count 60 :dimA "A1"
                 :child-cols [{:dimC "C1" :count 20} {:dimC "C2" :count 40}]
                 :child-rows [{:dimB "B1" :count 10 :child-cols [{:dimC "C1" :count 5} {:dimC "C2" :count 5}]}
                              {:dimB "B2" :count 50 :child-cols [{:dimC "C1" :count 15} {:dimC "C2" :count 35}]}]}
                {:count 40 :dimA "A2"
                 :child-cols [{:dimC "C1" :count 15} {:dimC "C2" :count 25}]}]
               (execute-query
                driver
                {:cube "wikiticker"
                 :splits [{:name "dimA" :on "rows"} {:name "dimB" :on "rows"} {:name "dimC" :on "columns"}]
                 :measures [{:name "count" :expression "(sum $count)"}]
                 :filters [{:interval ["2015" "2016"]}]
                 :totals true}
                cube-metadata)))))))
