(ns pivot.druid-test
  (:require [clojure.test :refer :all]
            [stub-http.core :refer :all]
            [pivot.asserts :refer [submaps?]]
            [pivot.druid :refer [datasources dimensions metrics]]
            [clojure.java.io :as io]))

(defn druid-res [name]
  {:status 200
   :content-type "application/json"
   :body (-> (str "druid_responses/" name ".json") io/resource slurp)})

(deftest datasources-test
  (with-routes!
    {{:method :get :path "/druid/v2/datasources"} (druid-res "datasources")}
    (is (= [{:name "ds1"} {:name "ds2"}] (datasources uri)))))

(deftest dimensions-test
  (with-routes!
    {{:method :post :path "/druid/v2"} (druid-res "segment_metadata")}
    (is (submaps? [{:name "__time"} {:name "cityName"} {:name "countryName"}]
                  (dimensions uri "wikiticker")))))

(deftest metrics-test
  (with-routes!
    {{:method :post :path "/druid/v2"} (druid-res "segment_metadata")}
    (is (submaps? [{:name "added"} {:name "count"}]
                  (metrics uri "wikiticker")))))
