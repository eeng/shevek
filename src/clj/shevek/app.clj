(ns shevek.app
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]]
            [shevek.config :refer [env]]
            [shevek.server]
            [taoensso.timbre :as log])
  (:gen-class))

(defn start-nrepl [port]
  (log/info "Starting nrepl server on http://localhost:" port)
  (start-server :port port))

(defstate ^{:on-reload :noop} nrepl
  :start (start-nrepl (env :nrepl-port))
  :stop (stop-server nrepl))

(defn -main [& args]
  (mount/start))
