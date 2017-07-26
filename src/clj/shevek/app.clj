(ns shevek.app
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]]
            [shevek.config :refer [config env? env]]
            [shevek.web.server :refer [web-server]]
            [shevek.db :refer [db]]
            [shevek.dw]
            [shevek.schema.refresher :refer [refresher]]
            [shevek.schema.seed :refer [seed!]]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [schema.core :as s])
  (:gen-class))

(defn start-nrepl [port]
  (log/info (str "Starting nrepl server on http://localhost:" port))
  (start-server :port port))

(defstate nrepl
  :start (start-nrepl (config :nrepl-port))
  :stop (stop-server nrepl))

(defstate initializer :start
  (do
    (log/merge-config!
     (cond-> {:timestamp-opts {:pattern "yy-MM-dd HH:mm:ss.SSS"}} ; Por defecto no pone los msegs
             (env? :test) (assoc :appenders {:println {:enabled? false}
                                             :spit (appenders/spit-appender {:fname "log/test.log"})})))
    (s/set-fn-validation! (not (env? :production)))
    (log/info "Starting app in" (env) "environment")))

(defn start-without-nrepl []
  (mount/start-without #'nrepl))

(defn start-db []
  (mount/start-without #'nrepl #'web-server #'refresher))

(defn start []
  (mount/start))

(defn seed []
  (start-db)
  (seed! db))

(defn -main [& args]
  (start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. mount/stop)))
