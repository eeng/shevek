(ns shevek.dw.druid-driver
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [shevek.logging :refer [pp-str]]))

(defrecord Druid [uri])

(defn connect [uri]
  (Druid. uri))

; TODO LOW http-kit viene con un client, ver si no se puede usar para no tener q agregar otra dependencia
(defn datasources [{:keys [uri]}]
  (:body (http/get (str uri "/druid/v2/datasources") {:as :json :conn-timeout 10000})))

; TODO en el :context de la q se le puede pasar un timeout
(defn send-query [{:keys [uri]} dq]
  (log/debug "Sending query to druid:\n" (pp-str dq))
  (:body (http/post (str uri "/druid/v2") {:content-type :json :form-params dq :as :json})))
