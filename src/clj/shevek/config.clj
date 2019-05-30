(ns shevek.config
  (:require [mount.core :refer [defstate running-states]]
            [cprop.core :refer [load-config]]
            [schema.core :as s]
            [shevek.schemas.config :refer [Config]]
            [shevek.lib.collections :refer [wrap-coll]]))

(defstate cfg
  :start (s/validate Config (load-config))
  :stop :stopped) ; Helps with reloading on case of config errors

(defn state-started? [state-var]
  (contains? (running-states) (str state-var)))

(defn config
  ([key]
   (config key nil))
  ([key default-value]
   {:pre [(state-started? #'cfg)]}
   (get-in cfg (wrap-coll key) default-value)))

#_(mount.core/start #'cfg)
#_(mount.core/stop #'cfg)
#_(config :env)
