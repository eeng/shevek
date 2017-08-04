(ns shevek.lib.druid-driver
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [shevek.logging :refer [pp-str]]
            [cheshire.core :refer [parse-string]]))

(defprotocol DruidDriver
  (datasources [this])
  (send-query [this q]))

(defrecord Druid [uri]
  DruidDriver

  ; TODO LOW http-kit viene con un client, ver si no se puede usar para no tener q agregar otra dependencia
  (datasources [_]
    (:body (http/get (str uri "/druid/v2/datasources") {:as :json :conn-timeout 10000})))

  (send-query [_ dq]
    (log/debug "Sending query to druid:\n" (pp-str dq))
    (try
      (:body (http/post (str uri "/druid/v2") {:content-type :json :form-params dq :as :json}))
      (catch clojure.lang.ExceptionInfo e
        (throw (ex-info "Druid error" (-> e ex-data :body (parse-string true))))))))

(defn connect [uri]
  (Druid. uri))
