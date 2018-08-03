(ns shevek.lib.druid-driver
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :refer [parse-string generate-string]]))

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

  (send-query [_ dq]
    (log/debug "Sending query to Druid:" (generate-string dq))
    (request http/post (str uri "/druid/v2") {:content-type :json :form-params dq :as :json})))

(defn connect [uri]
  (Druid. uri))
