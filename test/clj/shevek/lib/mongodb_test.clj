(ns shevek.lib.mongodb-test
  (:require [clojure.test :refer :all]
            [shevek.test-helper :refer [it]]
            [shevek.asserts :refer [submap?]]
            [shevek.lib.mongodb :refer [timestamp]]
            [clj-time.core :refer [date-time now]]))

(deftest timestamp-tests
  (testing "should set created-at if not present"
    (with-redefs [now (constantly (date-time 2017))]
      (is (submap? {:created-at (date-time 2017)} (timestamp {})))
      (is (submap? {:created-at (date-time 2018)} (timestamp {:created-at (date-time 2018)})))))

  (testing "should always set updated-at"
    (with-redefs [now (constantly (date-time 2017))]
      (is (submap? {:updated-at (date-time 2017)} (timestamp {})))
      (is (submap? {:updated-at (date-time 2017)} (timestamp {:updated-at (date-time 2016)}))))))
