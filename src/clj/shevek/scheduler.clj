(ns shevek.scheduler
  (:require [mount.core :refer [defstate]]
            [taoensso.timbre :refer [info error]]
            [overtone.at-at :as at :refer [mk-pool stop-and-reset-pool!]]
            [shevek.config :refer [config]]
            [shevek.schema.seed :as seed]
            [shevek.schema.manager :as m]
            [shevek.engine.state :refer [dw]]
            [shevek.lib.logging :refer [benchmark]]
            [shevek.reports.repository :refer [delete-old-shared-reports]]
            [shevek.db :refer [db]]))

(defn- wrap-error [f]
  (try
    (f)
    (catch Exception e
      (error e))))

(defn- every [interval fun pool & args]
  (apply at/every interval #(wrap-error fun) pool args))

(defn- configure-datasources-discovery-task [pool]
  (let [interval (config :datasources-discovery-interval)]
    (if (pos? interval)
      (do
        (info "Starting auto discovery task, will execute every" interval "secs")
        (every (* interval 1000) #(m/seed-schema! db dw {:discover? true}) pool))
      (do
        (info "Auto discovery disabled")
        (m/seed-schema! db dw {:discover? false})))))

(defn- configure-time-boundary-updating-task [pool]
  (let [interval (config :time-boundary-update-interval)]
    (when (pos? interval)
      (info "Starting time boundary update task, will execute every" interval "secs")
      (every (* interval 1000)
             (partial m/update-time-boundary! dw db)
             pool
             :initial-delay (* interval 1000)))))

(defn- configure-old-shared-reports-purge-task [pool]
  (every (* 24 60 60 1000)
         (fn []
           (benchmark {:after "Old shared repors purged (%.0f ms)"})
           (delete-old-shared-reports db))
         pool))

(defn start! []
  (let [pool (at/mk-pool)]
    (seed/users db)
    (configure-datasources-discovery-task pool)
    (configure-time-boundary-updating-task pool)
    (configure-old-shared-reports-purge-task pool)
    pool))

(defstate scheduler
  :start (start!)
  :stop (stop-and-reset-pool! scheduler))
