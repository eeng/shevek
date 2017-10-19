(ns shevek.querying.expansion-test
  (:require [clojure.test :refer :all]
            [shevek.querying.expansion :refer [expand-query]]))

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
    (is (= [{:name "año" :operator "include" :value #{"2015"} :column "__time" :type "STRING"
             :extraction [{:type "timeFormat" :format "Y"}]}]
           (:splits
            (expand-query {:splits [{:name "año" :operator "include" :value #{"2015"}}]}
                          {:dimensions [{:name "año" :column "__time" :type "STRING"
                                         :extraction [{:type "timeFormat" :format "Y"}]}]})))))

  (testing "should set the schema default-time-zone if not present"
    (is (= "America/Lima"
           (:time-zone (expand-query {} {:default-time-zone "America/Lima"}))))
    (is (= "America/Buenos_Aires"
           (:time-zone (expand-query {:time-zone "America/Buenos_Aires"} {:default-time-zone "America/Lima"}))))))
