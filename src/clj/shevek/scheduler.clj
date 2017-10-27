(ns shevek.scheduler
  (:require [mount.core :refer [defstate]]
            [taoensso.timbre :refer [info error]]
            [overtone.at-at :as at :refer [mk-pool every stop-and-reset-pool!]]
            [shevek.config :refer [config]]
            [shevek.schema.seed :as seed]
            [shevek.schema.manager :as m]
            [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]))

(defn- wrap-error [f]
  (try
    (f)
    (catch Exception e
      (error e))))

(defn- refresh-schema []
  (m/discover! dw db)
  (seed/cubes db))

(defn- initial-schema-discovery [dri]
  (if (pos? dri)
    (info "Starting auto discovery process, will execute every" dri "msecs")
    (do
      (info "Auto discovery disabled")
      (seed/cubes db))))

(defn start! []
  (let [pool (at/mk-pool)
        dri (config :datasources-refresh-interval)]
    (future (wrap-error (fn []
                          (seed/users db)
                          (initial-schema-discovery dri))))
    (when (pos? dri)
      (every (* dri 1000) #(wrap-error refresh-schema) pool))
    (every 30000 #(wrap-error (partial m/update-time-boundary! dw db)) pool :initial-delay 10000)
    pool))

(defstate scheduler
  :start (start!)
  :stop (stop-and-reset-pool! scheduler))
