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
  (seed/users db)
  (m/discover! dw db)
  (seed/cubes db))

; TODO si esta en cero el refresh-interval, no me convence hacer el discovery. Eso no permitiria configurar totalmente el schema en la config. Si los users deberiamos seedearlos la primera vez siempre
(defn start! []
  (let [sch (hs/scheduler {})
        dri (config :datasources-refresh-interval)]
    (if (pos? dri)
      (info "Starting auto discovery process, will execute every" dri "msecs")
      (info "Auto discovery disabled, executing once"))
    (future (wrap-error refresh-schema))

    (when (pos? dri)
      (hs/add-task sch :refresh-schema {:handler #(wrap-error refresh-schema)
                                        :schedule (format "/%d * * * * * *" dri)}))
    (hs/add-task sch :update-time-boundary {:handler #(wrap-error (partial m/update-time-boundary! dw db))
                                            :schedule "/30 * * * * * *"})
    (hs/start! sch)))

(defstate scheduler
  :start (start!)
  :stop (hs/stop! scheduler))
