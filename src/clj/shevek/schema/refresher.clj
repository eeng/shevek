(ns shevek.schema.refresher
  (:require [clojure.core.async :refer [go chan alt! timeout <! put!]]
            [mount.core :refer [defstate]]
            [taoensso.timbre :refer [info error]]
            [shevek.config :refer [config]]
            [shevek.schema.manager :as schema]
            [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]))

(defn refresh []
  (try
    (schema/discover! dw db)
    (catch Exception e
      (error e))))

(defn start [every]
  (let [stop-ch (chan)
        enabled? (pos? every)]
    (if enabled?
      (info "Starting auto discovery process, will execute every" every "msecs")
      (info "Auto discovery disabled"))
    (when enabled?
      (go
        (while (alt! stop-ch false :default :keep-going)
          (<! (timeout every))
          (refresh))))
    stop-ch))

(defn stop [ch]
  (put! ch :stop))

(defstate refresher
  :start (start (* (config :datasources-refresh-interval) 1000))
  :stop (stop refresher))
