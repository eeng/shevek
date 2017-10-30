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

(defn- configure-datasources-discovery-task [pool]
  (let [interval (config :datasources-discovery-interval)]
    (if (pos? interval)
      (do
        (info "Starting auto discovery task, will execute every" interval "secs")
        (every (* interval 1000) #(wrap-error refresh-schema) pool))
      (do
        (info "Auto discovery disabled")
        (seed/cubes db)))))

(defn- configure-time-boundary-updating-task [pool]
  (let [interval (config :time-boundary-update-interval)]
    (when (pos? interval)
      (info "Starting time boundary update task, will execute every" interval "secs")
      (every (* interval 1000) #(wrap-error (partial m/update-time-boundary! dw db)) pool :initial-delay (* interval 1000)))))

(defn start! []
  (let [pool (at/mk-pool)]
    (seed/users db)
    (configure-datasources-discovery-task pool)
    (configure-time-boundary-updating-task pool)
    pool))

(defstate scheduler
  :start (start!)
  :stop (stop-and-reset-pool! scheduler))
