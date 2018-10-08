(ns shevek.engine.druid.driver-test
  (:require [clojure.test :refer :all]
            [stub-http.core :refer :all]
            [shevek.asserts :refer [submaps?]]
            [shevek.engine.druid.driver :as driver]
            [shevek.support.druid :refer [druid-res]]
            [cheshire.core :refer [generate-string]]))

(deftest datasources-test
  (with-routes!
    {{:method :get :path "/druid/v2/datasources"} (druid-res "datasources")}
    (is (= ["ds1" "ds2"] (driver/datasources (driver/http-druid-driver uri))))))

(deftest send-query-test
  (testing "successful request"
    (with-routes!
      {(fn [{:keys [method path body headers]}]
         (and (= method "POST")
              (= path "/druid/v2")
              (= (body "postData") (generate-string {:queryType "timeBoundary"}))
              (= (:content-type headers) "application/json")))
       (druid-res "time-boundary")}
      (is (= [{:timestamp "T1" :result {:maxTime "T2"}}]
             (driver/send-query (driver/http-druid-driver uri) {:queryType "timeBoundary"})))))

  (testing "timeout response"
    (with-routes!
      {{:method :post :path "/druid/v2"} (assoc (druid-res "timeout") :status 500)}
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Druid error"
                            (driver/send-query (driver/http-druid-driver uri) {:queryType "timeBoundary"}))))))
