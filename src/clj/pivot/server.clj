(ns pivot.server
  (:require [mount.core :refer [defstate]]
            [org.httpkit.server :refer [run-server]]
            [environ.core :refer [env]]))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "hello HTTP!"})

(defn start-web-server []
  (let [port (Integer. (env :http-port))]
    (println (str "Web server on http://localhost:" port))
    (run-server app {:port port})))

(defstate web-server
  :start (start-web-server)
  :stop (web-server :timeout 100))
