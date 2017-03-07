(ns pivot.config
  (:require [mount.core :as mount :refer [defstate]]
            [cprop.core :refer [load-config]]))

(defstate cfg :start (load-config))

(defn env [& keys]
  (get-in cfg keys))
