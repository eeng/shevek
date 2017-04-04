(ns shevek.dw.druid-driver-test
  (:require [clojure.test :refer :all]
            [stub-http.core :refer :all]
            [shevek.asserts :refer [submaps?]]
            [shevek.dw.druid-driver :as druid]
            [clojure.java.io :as io]
            [cheshire.core :refer [generate-string]]))

(defn druid-res [name]
  {:status 200
   :content-type "application/json"
   :body (-> (str "druid_responses/" name ".json") io/resource slurp)})

(deftest datasources-test
  (with-routes!
    {{:method :get :path "/druid/v2/datasources"} (druid-res "datasources")}
    (is (= ["ds1" "ds2"] (druid/datasources (druid/connect uri))))))

(deftest send-query-test
  (with-routes!
    {(fn [{:keys [method path body headers]}]
       (and (= method "POST")
            (= path "/druid/v2")
            (= (body "postData") (generate-string {:queryType "timeBoundary"}))
            (= (:content-type headers) "application/json")))
     (druid-res "time-boundary")}
    (is (= [{:timestamp "T1"
             :result {:maxTime "T2"}}] 
           (druid/send-query (druid/connect uri) {:queryType "timeBoundary"})))))
