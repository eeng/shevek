(ns shevek.monitoring.middleware
  (:require [influxdb-clojure.core :as influxdb]
            [mount.core :refer [defstate]]
            [shevek.config :refer [config]]))

(defstate conn
  :start (when-let [{:keys [uri username password]} (config [:monitoring :influx])]
           (influxdb/connect uri username password)))

(defn write-point [point]
  (when conn
    (let [{:keys [database retention-policy] :or {retention-policy "autogen"}} (config [:monitoring :influx])]
      (future
        (influxdb/write-points conn database [point] {:retention-policy retention-policy})))))

(defn wrap-stats [handler]
  (fn [request]
    (let [start (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start)]
      (write-point {:measurement "shevek_requests"
                    :fields {:duration duration}
                    :tags {:host (.getHostName (java.net.InetAddress/getLocalHost))}})
      ; Add the duration to the response so the logging middleware can use it
      (assoc response :duration duration))))
