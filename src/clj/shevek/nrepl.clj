(ns shevek.nrepl
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [clojure.java.io :as io]
            [mount.core :refer [defstate]]
            [shevek.config :refer [config env?]]
            [taoensso.timbre :as log]))

(defn- create-nrepl-file
  "ProtoREPL detects the this file so there is no need to type it"
  [port]
  (when (env? :development)
    (doto (io/file ".nrepl-port")
      (spit port)
      (.deleteOnExit))))

(defn start-nrepl [port]
  (log/info "Starting nREPL server on port" port)
  (create-nrepl-file port)
  (start-server :port port))

(defstate ^{:on-reload :noop} nrepl
  :start (start-nrepl (config :nrepl-port))
  :stop (stop-server nrepl))
