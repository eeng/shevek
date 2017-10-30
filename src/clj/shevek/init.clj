(ns shevek.init
  (:require [mount.core :refer [defstate]]
            [taoensso.timbre :as log]
            [schema.core :as shema]
            [shevek.config :refer [env config]]
            [shevek.lib.logging :refer [configure-logging!]]))

(defstate initializer :start
  (do
    (configure-logging!)
    (shema/set-fn-validation! true)
    (log/info "Starting app in" (env) "environment")
    (log/info "Notification config for errors:" (config [:notifications :errors]))
    :done))
