(ns shevek.init
  (:require [mount.core :refer [defstate]]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [shevek.config :refer [env? env]]
            [schema.core :as s]))

(defstate initializer :start
  (do
    (log/merge-config!
     (cond-> {:timestamp-opts {:pattern "yy-MM-dd HH:mm:ss.SSS"}} ; Por defecto no pone los msegs
             (env? :test) (assoc :appenders {:println {:enabled? false}
                                             :spit (appenders/spit-appender {:fname "log/test.log"})})))
    (s/set-fn-validation! true)
    (log/info "Starting app in" (env) "environment")
    :done))
