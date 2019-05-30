(ns shevek.driver.http-druid
  (:require [shevek.driver.druid :refer [DruidDriver datasources]]
            [clj-http.client :as http]
            [cheshire.core :refer [parse-string generate-string]]
            [shevek.lib.logging :refer [benchmark]]))

(defn- request [method-fn uri opts]
  (try
    (:body (method-fn uri opts))
    (catch java.net.ConnectException e
      (throw (ex-info "Can't connect to Druid" {:uri uri})))
    (catch clojure.lang.ExceptionInfo e
      (throw (ex-info "Druid error" (-> e ex-data :body (parse-string true)))))))

(defrecord HttpDruidDriver [uri]
  DruidDriver

  (datasources [_]
    (request http/get (str uri "/druid/v2/datasources") {:as :json :conn-timeout 10000}))

  (send-query [_ {:keys [query] :as dq}]
    (let [endpoint (str uri (if query "/druid/v2/sql" "/druid/v2"))]
      (benchmark {:before (str "Sending query to Druid: " (generate-string dq {:pretty true}))
                  :after "Query finished in %.0f ms"
                  :log-level :debug}
        (request http/post endpoint {:content-type :json :form-params dq :as :json})))))

(defn http-druid-driver [uri]
  (HttpDruidDriver. uri))

#_(datasources (http-druid-driver "http://localhost:8082"))
