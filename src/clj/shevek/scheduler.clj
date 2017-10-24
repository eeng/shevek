(ns shevek.scheduler
  (:require [hara.io.scheduler :as hs]
            [mount.core :refer [defstate]]
            [taoensso.timbre :refer [info error]]
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
    (do
      (info "Starting auto discovery process, will execute every" dri "msecs")
      (refresh-schema))
    (do
      (info "Auto discovery disabled")
      (seed/cubes db))))

(defn start! []
  (let [sch (hs/scheduler {})
        dri (config :datasources-refresh-interval)]
    (future (wrap-error (fn []
                          (seed/users db)
                          (initial-schema-discovery dri))))

    (when (pos? dri)
      (hs/add-task sch :refresh-schema {:handler #(wrap-error refresh-schema)
                                        :schedule (format "/%d * * * * * *" dri)}))
    (hs/add-task sch :update-time-boundary {:handler #(wrap-error (partial m/update-time-boundary! dw db))
                                            :schedule "/30 * * * * * *"})
    (hs/start! sch)))

(defstate scheduler
  :start (start!)
  :stop (hs/stop! scheduler))
