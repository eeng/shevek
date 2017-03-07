(ns pivot.druid-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [stub-http.core :refer :all]
            [pivot.druid :refer [datasources]]))

(defn json-res [body]
  {:status 200 :content-type "application/json" :body (json/write-str body)})

(deftest datasources-test
  (with-routes!
    {{:method :get :path "/druid/v2/datasources"} (json-res ["ds1" "ds2"])}
    (is (= [{:name "ds1"} {:name "ds2"}] (datasources uri)))))
