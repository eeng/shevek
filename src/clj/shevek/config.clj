(ns shevek.config
  (:require [mount.core :refer [defstate]]
            [cprop.core :refer [load-config]]
            [schema.core :as s]
            [shevek.schemas.config :refer [Config]]
            [shevek.lib.collections :refer [wrap-coll]]))

(defstate cfg
  :start (s/validate Config (load-config))
  :stop :stopped) ; Helps with reloading on case of config errors

(defn config
  ([key]
   (or (config key nil)
       (throw (Exception. (str "Configuration error, property missing: " key)))))
  ([key default-value]
   (get-in cfg (wrap-coll key) default-value)))

(defn env []
  (config :env :production))

(defn env? [env-kw]
  (= (env) env-kw))
