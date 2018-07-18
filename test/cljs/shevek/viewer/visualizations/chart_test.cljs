(ns shevek.viewer.visualizations.chart-test
  (:require-macros [cljs.test :refer [deftest testing is are]])
  (:require [pjstadig.humane-test-output]
            [shevek.asserts :refer [submap? submaps?]]
            [shevek.viewer.visualizations.chart :refer [build-chart-data]]))

(deftest build-chart-data-tests
  (testing "labels"
    (is (submap? {:labels ["Argentina" "Brasil"]}
                 (build-chart-data {:name "added"}
                                   {:splits [{:name "country"}]
                                    :results [{}
                                              {:country "Argentina"}
                                              {:country "Brasil"}]}))))

  (testing "datasets"
    (testing "one split and viztype bar-chart"
      (is (= [{:data [100 200] :label "Added" :backgroundColor ["#42a5f5" "#ff7043"]}]
             (:datasets
              (build-chart-data {:name "added" :title "Added"}
                                {:splits [{:name "country"}]
                                 :results [{:added 300}
                                           {:added 100 :country "Argentina"}
                                           {:added 200 :country "Brasil"}]
                                 :viztype :bar-chart})))))

    (testing "one split and viztype line-chart"
      (is (= [{:data [100 200] :label "Added" :borderColor "#42a5f5" :backgroundColor "rgba(66, 165, 245, 0.3)"}]
             (:datasets
              (build-chart-data {:name "added" :title "Added"}
                                {:splits [{:name "country"}]
                                 :results [{:added 300}
                                           {:added 100 :country "Argentina"}
                                           {:added 200 :country "Brasil"}]
                                 :viztype :line-chart})))))

    (testing "two splits with equal size of nested results"
      (is (submaps? [{:data [20 160] :nestedLabels ["Santa Fe" "Sao Paulo"] :backgroundColor "#42a5f5"}
                     {:data [80 40] :nestedLabels ["Rafaela" "Brasilia"] :backgroundColor "#ff7043"}]
                    (:datasets
                     (build-chart-data
                      {:name "added" :title "Added"}
                      {:splits [{:name "country"} {:name "city"}]
                       :results [{}
                                 {:country "Argentina" :child-rows [{:added 20 :city "Santa Fe"}
                                                                    {:added 80 :city "Rafaela"}]}
                                 {:country "Brasil" :child-rows [{:added 160 :city "Sao Paulo"}
                                                                 {:added 40 :city "Brasilia"}]}]
                       :viztype :bar-chart})))))

    (testing "two splits with different size of nested results"
      (is (submaps? [{:data [30 70] :nestedLabels ["Santa Fe" "Sao Paulo"]}
                     {:data [80 40] :nestedLabels ["Rafaela" "Brasilia"]}
                     {:data [25 nil] :nestedLabels ["Ceres" nil]}]
                    (:datasets
                     (build-chart-data
                      {:name "added" :title "Added"}
                      {:splits [{:name "country"} {:name "city"}]
                       :results [{}
                                 {:country "Argentina" :child-rows [{:added 30 :city "Santa Fe"}
                                                                    {:added 80 :city "Rafaela"}
                                                                    {:added 25 :city "Ceres"}]}
                                 {:country "Brasil" :child-rows '({:added 70 :city "Sao Paulo"}
                                                                  {:added 40 :city "Brasilia"})}]})))))))
