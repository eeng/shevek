(ns shevek.lib.druid-driver
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [shevek.lib.logging :refer [pp-str]]
            [cheshire.core :refer [parse-string]]))

(defprotocol DruidDriver
  (datasources [this])
  (send-query [this q]))

(derive ::druid-error :shevek.app/error)

(defrecord Druid [uri]
  DruidDriver

  (datasources [_]
    (:body (http/get (str uri "/druid/v2/datasources") {:as :json :conn-timeout 10000})))

  (send-query [_ dq]
    (log/debug "Sending query to druid:\n" (pp-str dq))
    (try
      (:body (http/post (str uri "/druid/v2") {:content-type :json :form-params dq :as :json}))
      (catch clojure.lang.ExceptionInfo e
        (throw (ex-info "Druid error" (-> e ex-data :body (parse-string true)
                                          (assoc :type ::druid-error))))))))

(defn connect [uri]
  (Druid. uri))
