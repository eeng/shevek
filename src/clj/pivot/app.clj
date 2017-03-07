(ns pivot.app
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]]
            [pivot.server])
  (:gen-class))

(defstate ^{:on-reload :noop} nrepl
  :start (start-server :port 3101)
  :stop (stop-server nrepl))

(defn -main [& args]
  (mount/start))
