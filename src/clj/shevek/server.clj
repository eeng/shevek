(ns shevek.server
  (:require [mount.core :refer [defstate]]
            [org.httpkit.server :refer [run-server]]
            [shevek.config :refer [env]]
            [shevek.handler :refer [app]]))

; TODO reemplazar el println x logging
(defn start-web-server [port]
  (println (str "Starting web server on http://localhost:" port))
  (run-server app {:port port}))

(defstate web-server
  :start (start-web-server (env :port))
  :stop (web-server :timeout 100))
