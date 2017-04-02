(ns shevek.config
  (:require [mount.core :refer [defstate]]
            [cprop.core :refer [load-config]]))

(defstate cfg :start (load-config))

(defn config [& keys]
  (get-in cfg keys))

(defn env []
  (config :env))

(defn env? [env-kw]
  (= (env) env-kw))
