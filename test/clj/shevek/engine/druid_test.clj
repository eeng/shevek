(ns shevek.engine.druid-test
  (:require [clojure.test :refer :all]
            [clj-fakes.core :as f]
            [shevek.asserts :refer [submap-arg]]
            [shevek.lib.time :refer [parse-time]]
            [shevek.engine.protocol :as e]
            [shevek.engine.druid :refer [druid-engine]]
            [shevek.engine.druid.driver :refer [DruidDriver]]))

(deftest cubes-test
  (testing "returns a list of all datasources"
    (f/with-fakes
      (let [driver (f/reify-fake DruidDriver
                     (datasources :fake [[] ["sales" "inventory"]]))]
        (is (= ["sales" "inventory"]
               (e/cubes (druid-engine driver))))))))

(deftest cube-metadata-test
  (testing "should issue Druid queries to gatter the dimensions, measures and time boundary"
    (f/with-fakes
      (let [driver (f/reify-fake DruidDriver
                     (send-query
                      :fake
                      [[(submap-arg {:queryType "timeBoundary" :dataSource "wikiticker"})]
                       [{:result {:maxTime "2015-09-12T23:59:59.200Z"}}]

                       [(submap-arg {:queryType "segmentMetadata" :dataSource "wikiticker"})]
                       [{:columns {:region {:type "STRING"} :city {:type "STRING"} :added {:type "LONG"}}
                         :aggregators {:added {:type "longSum"}}}]]))]

        (is (= {:dimensions [{:name "region" :type "STRING"} {:name "city" :type "STRING"}]
                :measures [{:name "added" :type "longSum"}]
                :max-time (parse-time "2015-09-12T23:59:59.200Z")
                :min-time nil}
               (e/cube-metadata (druid-engine driver) "wikiticker")))))))

(deftest custom-query-testing
  (testing "sends a Druid SQL query"
    (f/with-fakes
      (let [driver (f/reify-fake DruidDriver
                     (send-query :fake [[{:query "SELECT sum(added) from wikiticker"}]
                                        [{:EXPR0 9385573}]]))]
        (is (= [{:EXPR0 9385573}]
               (e/custom-query (druid-engine driver)
                               "SELECT sum(added) from wikiticker")))))))
