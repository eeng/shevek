(ns shevek.querying.expansion-test
  (:require [clojure.test :refer :all]
            [shevek.querying.expansion :refer [expand-query]]
            [shevek.lib.time :refer [date-time now]]))

(deftest expand-query-test
  (testing "should gather measures information from the schema"
    (is (= [{:name "m1" :expression "(avg $m1)"}
            {:name "m3" :expression "(sum $m3)"}]
           (:measures
            (expand-query {:measures ["m1" "m3"]}
                          {:measures [{:name "m1" :expression "(avg $m1)" :title "..."}
                                      {:name "m2" :expression "..." :title "..."}
                                      {:name "m3" :expression "(sum $m3)" :title "..."}]})))))

  (testing "should gather dimensions information from the schema for the filters"
    (is (= [{:name "año" :operator "include" :value #{"2015"} :column "__time"
             :extraction [{:type "timeFormat" :format "Y"}]}]
           (:filters
            (expand-query {:filters [{:name "año" :operator "include" :value #{"2015"}}]}
                          {:dimensions [{:name "año" :column "__time"
                                         :extraction [{:type "timeFormat" :format "Y"}]}]})))))

  (testing "should gather dimensions information from the schema for the splits"
    (is (= [{:name "año" :operator "include" :value #{"2015"} :column "__time"
             :extraction [{:type "timeFormat" :format "Y"}]}]
           (:splits
            (expand-query {:splits [{:name "año" :operator "include" :value #{"2015"}}]}
                          {:dimensions [{:name "año" :column "__time"
                                         :extraction [{:type "timeFormat" :format "Y"}]}]})))))

  (testing "should gather dimension/measure information from the schema for the sort-by in the splits"
    (is (= [{:name "d" :descending true :type "LONG"}
            {:name "m" :descending false :expression "(sum $m)"}]
           (->> (expand-query {:splits [{:name "d" :sort-by {:name "d" :descending true}}
                                        {:name "e" :sort-by {:name "m" :descending false}}]}
                              {:dimensions [{:name "d" :type "LONG"}]
                               :measures [{:name "m" :expression "(sum $m)"}]})
                :splits (map :sort-by)))))

  (testing "should set the schema default-time-zone if not present"
    (is (= "America/Lima"
           (:time-zone (expand-query {} {:default-time-zone "America/Lima"}))))
    (is (= "America/Buenos_Aires"
           (:time-zone (expand-query {:time-zone "America/Buenos_Aires"} {:default-time-zone "America/Lima"})))))

  (testing "should convert relative periods to absolute intervals"
    (with-redefs [now (constantly (date-time 2017 3 15 10 30))]
      (is (= ["2017-03-15T00:00:00.000Z" "2017-03-15T23:59:59.999Z"]
             (->> (expand-query {:filters [{:period "current-day"}]} {})
                  :filters first :interval))))

    (is (= ["2017-03-05T17:29:00.000Z" "2017-03-06T17:29:00.000Z"]
           (->> (expand-query {:filters [{:period "latest-day"}]} {:max-time "2017-03-06T17:28"})
                :filters first :interval))))

  (testing "should expand absolute interval to"
    (is (= ["2017-01-01T03:00:00.000Z" "2018-01-01T23:59:59.999Z"]
           (->> (expand-query {:filters [{:interval ["2017" "2018"]}]} {})
                :filters first :interval)))))

  ; TODO should respect query and default-time-zone

  ; TODO max-time should be stored on the db
