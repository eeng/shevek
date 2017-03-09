(ns pivot.app
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [mount.core :as mount :refer [defstate]]
            [pivot.config :refer [env]]
            [pivot.server])
  (:gen-class))

; TODO reemplazar el println x logging
(defn start-nrepl [port]
  (println (str "Starting nrepl server on http://localhost:" port))
  (start-server :port port))

(defstate ^{:on-reload :noop} nrepl
  :start (start-nrepl (env :nrepl-port))
  :stop (stop-server nrepl))

(defn -main [& args]
  (mount/start))