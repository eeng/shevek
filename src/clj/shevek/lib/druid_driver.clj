(ns shevek.lib.druid-driver
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :refer [parse-string generate-string]]
            [shevek.lib.logging :refer [benchmark]]))

(defprotocol DruidDriver
  (datasources [this])
  (send-query [this q]))

(defn- request [method-fn uri opts]
  (try
    (:body (method-fn uri opts))
    (catch java.net.ConnectException e
      (throw (ex-info "Can't connect to Druid" {:uri uri})))
    (catch clojure.lang.ExceptionInfo e
      (throw (ex-info "Druid error" (-> e ex-data :body (parse-string true)))))))

(defrecord Druid [uri]
  DruidDriver

  (datasources [_]
    (request http/get (str uri "/druid/v2/datasources") {:as :json :conn-timeout 10000}))

  (send-query [_ {:keys [query] :as dq}]
    (let [endpoint (str uri (if query "/druid/v2/sql" "/druid/v2"))]
      (log/debug "Sending query to Druid:" (generate-string dq {:pretty true}))
      (benchmark "Query finished in %.0f ms"
        (request http/post endpoint {:content-type :json :form-params dq :as :json})))))

(defn connect [uri]
  (Druid. uri))
