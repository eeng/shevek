(ns shevek.dw.druid-driver
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [shevek.logging :refer [pp-str]]))

; So we can stub this functions when testing
(defprotocol DruidDriver
  (datasources [this])
  (send-query [this q]))

(defrecord Druid [uri]
  DruidDriver

  ; TODO LOW http-kit viene con un client, ver si no se puede usar para no tener q agregar otra dependencia
  (datasources [_]
    (:body (http/get (str uri "/druid/v2/datasources") {:as :json :conn-timeout 10000})))

  ; TODO en el :context de la q se le puede pasar un timeout
  (send-query [_ dq]
    (log/debug "Sending query to druid:\n" (pp-str dq))
    (:body (http/post (str uri "/druid/v2") {:content-type :json :form-params dq :as :json}))))

(defn connect [uri]
  (Druid. uri))
