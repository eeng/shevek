(ns shevek.schema.metadata-test
  (:require [clojure.test :refer :all]
            [shevek.lib.druid-driver :refer [datasources send-query]]
            [shevek.schema.metadata :as m]))

(deftest engine-test
  (testing "cubes should return a vector of datasources"
    (let [fake-dw "the dw"]
      (with-redefs [datasources (fn [dw]
                                  (is (= dw fake-dw))
                                  ["wikiticker" "another"])]
        (is (= ["wikiticker" "another"] (m/cubes fake-dw))))))

  (testing "dimensions-and-measures should return a pair of vectors, first the dimensions and second the measures"
    (with-redefs [send-query
                  (fn [_ {:keys [queryType dataSource]}]
                    (case [queryType dataSource]
                      ["timeBoundary" "wikiticker"]
                      [{:result {:maxTime "2015-09-12T23:59:59.200Z"}}]

                      ["segmentMetadata" "wikiticker"]
                      [{:columns {:region {:type "STRING"} :city {:type "STRING"} :added {:type "LONG"}}
                        :aggregators {:added {:type "longSum"}}}]))]
      (is (= [[{:name "region" :type "STRING"} {:name "city" :type "STRING"}]
              [{:name "added" :type "longSum"}]]
             (m/dimensions-and-measures nil "wikiticker"))))))
