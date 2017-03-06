(ns pivot.druid
  (:require [clj-http.client :as http]))

(def broker "http://kafka:8082/druid/v2")

(defn datasources [host]
  (:body (http/get (str host "/datasources") {:as :json})))

(datasources broker)
