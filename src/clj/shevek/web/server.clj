(ns shevek.web.server
  (:require [mount.core :refer [defstate]]
            [org.httpkit.server :refer [run-server]]
            [shevek.config :refer [config]]
            [shevek.web.handler :refer [app]]
            [taoensso.timbre :as log]))

(defn start-web-server [port]
  (log/info (str "Starting web server on http://localhost:" port))
  (run-server app {:port port}))

(defstate web-server
  :start (start-web-server (config :port))
  :stop (web-server :timeout 100))
