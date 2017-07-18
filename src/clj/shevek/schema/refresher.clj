(ns shevek.schema.refresher
  (:require [clojure.core.async :refer [go chan alt! timeout <! put!]]
            [mount.core :refer [defstate]]))

(defn do-refresh [])

(defn start [every]
  (let [stop-ch (chan)]
    (go
      (while (alt! stop-ch false :default :keep-going)
        (<! (timeout every))
        (do-refresh)))
    stop-ch))

(defn stop [ch]
  (put! ch :stop))

(defstate refresher
  :start (start 60000)
  :stop (stop refresher))
