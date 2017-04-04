(ns shevek.dw.engine-test
  (:require [clojure.test :refer :all]
            [shevek.dw.druid-driver :refer [datasources send-query]]
            [shevek.dw.engine :as e]))

(deftest engine-test
  (testing "cubes should return a vector of datasources"
    (let [fake-dw "the dw"]
      (with-redefs [datasources (fn [dw]
                                  (is (= dw fake-dw))
                                  ["wikiticker" "another"])]
        (is (= ["wikiticker" "another"] (e/cubes fake-dw))))))

  (testing "dimensions-and-measures should return a pair of vectors, first the dimensions and second the measures"
    (with-redefs [send-query
                  (fn [_ {:keys [queryType dataSource]}]
                    (is (= queryType "segmentMetadata"))
                    (is (= (dataSource :name) "wikiticker"))
                    [{:columns {:region {:type "STRING"}
                                :city {:type "STRING"}
                                :added {:type "LONG"}}
                      :aggregators {:added {:type "longSum"}}}])]
      (is (= [[{:name "region" :type "STRING"} {:name "city" :type "STRING"}]
              [{:name "added" :type "longSum"}]]
             (e/dimensions-and-measures nil "wikiticker"))))))
