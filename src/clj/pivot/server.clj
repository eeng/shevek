(ns pivot.server
  (:require [mount.core :refer [defstate]]
            [org.httpkit.server :refer [run-server]]))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "hello HTTP!"})

(defstate web-server
  :start (run-server app {:port 3100})
  :stop (web-server :timeout 100))
