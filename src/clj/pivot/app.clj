(ns pivot.app
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]])
  (:gen-class))

(defstate ^{:on-reload :noop} nrepl
  :start (start-server :port 4001)
  :stop (stop-server nrepl))

(defn -main [& args]
  (mount/start))
