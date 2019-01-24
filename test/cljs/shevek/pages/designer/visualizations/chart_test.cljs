(ns shevek.pages.designer.visualizations.chart-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [shevek.asserts :refer [submap? submaps?]]
            [shevek.pages.designer.visualizations.chart :refer [build-chart-data]]))

(deftest build-chart-data-tests
  (testing "one split and viztype bar-chart"
    (let [chart-data (build-chart-data {:name "added" :title "Added"}
                                       {:splits [{:name "country"}]
                                        :results [{:added 300}
                                                  {:added 100 :country "Argentina"}
                                                  {:added 200 :country "Brasil"}]
                                        :viztype :bar-chart})]
      (is (= ["Argentina" "Brasil"] (:labels chart-data)))
      (is (submaps? [{:data [100 200] :label nil :backgroundColor ["#42a5f5AA" "#ff7043AA"]}]
                    (:datasets chart-data)))))

  (testing "one split and viztype line-chart"
    (is (submaps? [{:data [100 200] :label nil :borderColor "#42a5f5" :backgroundColor "#42a5f522"}]
                  (:datasets
                   (build-chart-data {:name "added" :title "Added"}
                                     {:splits [{:name "country"}]
                                      :results [{:added 300}
                                                {:added 100 :country "Argentina"}
                                                {:added 200 :country "Brasil"}]
                                      :viztype :line-chart})))))

  (testing "two splits with same set of values on second split (and some missing values too)"
    (let [chart-data (build-chart-data
                      {:name "added" :title "Added"}
                      {:splits [{:name "day"} {:name "year" :on "columns"}]
                       :results [{:child-cols [{:year 2016 :added 99}
                                               {:year 2017 :added 99}
                                               {:year 2018 :added 99}]}
                                 {:day 1 :child-cols [{:year 2016 :added 16}
                                                      {:year 2018 :added 18}]}
                                 {:day 2 :child-cols [{:year 2017 :added 27}
                                                      {:year 2018 :added 28}]}
                                 {:day 3 :child-cols [{:year 2016 :added 36}
                                                      {:year 2017 :added 37}]}]})]
      (is (= [1 2 3] (:labels chart-data)))
      (is (submaps? [{:label 2016 :data [16 0 36]}
                     {:label 2017 :data [0 27 37]}
                     {:label 2018 :data [18 28 0]}]
                    (:datasets chart-data)))))

  (testing "two splits with empty results"
    (let [chart-data (build-chart-data {:name "added" :title "Added"}
                                       {:splits [{:name "country"} {:name "city"}]
                                        :results [{:added 1}]
                                        :viztype :bar-chart})]
      (is (= [] (:labels chart-data)))
      (is (= [] (:datasets chart-data))))))
