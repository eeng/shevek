(ns shevek.nrepl
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :refer [defstate]]
            [shevek.config :refer [config]]
            [taoensso.timbre :refer [info]]))

(defn start-nrepl [port]
  (info (str "Starting nrepl server on http://localhost:" port))
  (start-server :port port))

(defstate nrepl
  :start (start-nrepl (config :nrepl-port))
  :stop (stop-server nrepl))
