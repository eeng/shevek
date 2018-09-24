(ns shevek.querying.exporters.tsv-test
  (:require [clojure.test :refer :all]
            [clojure.data.csv :refer [read-csv]]
            [shevek.querying.exporters.tsv :as tsv]))

(defn read-excel-tsv [tsv]
  (let [in (java.io.StringReader. tsv)]
    (read-csv in :separator \tab :newline :cr+lf)))

(defn generate-and-parse [viz]
  (read-excel-tsv (tsv/generate viz)))

(deftest generate-test
  (testing "totals only"
    (is (= [["Added" "Count"]
            ["20" "10"]]
           (generate-and-parse
            {:measures [{:name "added" :title "Added"},
                        {:name "count" :title "Count"}]
             :results [{:count 10, :added 20}]}))))

  (testing "one row dim and one measure"
    (is (= [["Country" "Count"]
            ["Total" "10"]
            ["AR" "8"]
            ["CH" "2"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 10}
                       {:count 8 :country "AR"}
                       {:count 2 :country "CH"}]}))))

  (testing "two row dims and one measure"
    (is (= [["Country" "City" "Count"]
            ["Total" "" "10"]
            ["AR" "" "8"]
            ["AR" "cba" "5"]
            ["AR" "sfe" "3"]
            ["CH" "" "2"]
            ["CH" "sgo" "2"]]
           (generate-and-parse
            {:splits [{:name "country" :title "Country"}
                      {:name "city" :title "City"}]
             :measures [{:name "count" :title "Count"}]
             :results [{:count 10}
                       {:count 8 :country "AR" :child-rows [{:count 5 :city "cba"} {:count 3 :city "sfe"}]}
                       {:count 2 :country "CH" :child-rows [{:count 2 :city "sgo"}]}]})))))
