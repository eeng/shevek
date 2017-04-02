(ns shevek.app
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]]
            [shevek.config :refer [config env? env]]
            [shevek.server]
            [taoensso.timbre :as log]
            [schema.core :as s])
  (:gen-class))

(defn start-nrepl [port]
  (log/info "Starting nrepl server on http://localhost:" port)
  (start-server :port port))

(defstate ^{:on-reload :noop} nrepl
  :start (start-nrepl (config :nrepl-port))
  :stop (stop-server nrepl))

; Agrego los milisegundos y en test no quiero loggear nada
(defn- config-logger []
  (log/merge-config!
   (cond-> {:timestamp-opts {:pattern "yy-MM-dd HH:mm:ss.SSS"}}
           (env? :test) (assoc :appenders {:println {:enabled? false}})))
  (log/info "Starting app in" (env) "environment"))

(defstate initializer :start
  (do
    (config-logger)
    (s/set-fn-validation! (not (env? :production)))))

(defn -main [& args]
  (mount/start))
