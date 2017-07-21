(ns shevek.schema.refresher
  (:require [clojure.core.async :refer [go chan alt! timeout <! put!]]
            [mount.core :refer [defstate]]
            [taoensso.timbre :refer [info error]]
            [shevek.config :refer [config]]
            [shevek.schema.manager :refer [discover!]]
            [shevek.schema.seed :refer [seed!]]
            [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]))

(defn- refresh []
  (try
    (discover! dw db)
    (seed! db)
    (catch Exception e
      (error e))))

(defn- execute-every [every stop-ch]
  (info "Starting auto discovery process, will execute every" every "msecs.")
  (go
    (while (alt! stop-ch false :default :keep-going)
      (refresh)
      (<! (timeout every)))))

(defn- execute-once []
  (info "Auto discovery disabled, executing once.")
  (refresh))

(defn start [every]
  (let [stop-ch (chan)]
    (if (pos? every)
      (execute-every every stop-ch)
      (execute-once))
    stop-ch))

(defn stop [ch]
  (put! ch :stop))

(defstate refresher
  :start (start (* (config :datasources-refresh-interval) 1000))
  :stop (stop refresher))
