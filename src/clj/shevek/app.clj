(ns shevek.app
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]]
            [shevek.config :refer [config env? env]]
            [shevek.server]
            [shevek.db]
            [taoensso.timbre :as log]
            [schema.core :as s])
  (:gen-class))

(defn start-nrepl [port]
  (log/info "Starting nrepl server on http://localhost:" port)
  (start-server :port port))

(defstate ^{:on-reload :noop} nrepl
  :start (start-nrepl (config :nrepl-port))
  :stop (stop-server nrepl))

(defstate initializer :start
  (do
    (log/merge-config!
     (cond-> {:timestamp-opts {:pattern "yy-MM-dd HH:mm:ss.SSS"}} ; Por defecto no pone los msegs
             (env? :test) (assoc :appenders {:println {:enabled? false}})))
    (s/set-fn-validation! (not (env? :production)))
    (log/info "Starting app in" (env) "environment")))

(defn -main [& args]
  (mount/start))
