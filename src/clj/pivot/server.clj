(ns pivot.server
  (:require [mount.core :refer [defstate]]
            [org.httpkit.server :refer [run-server]]
            [pivot.config :refer [env]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [files not-found]]))

(defroutes app-routes
  (GET "/" [] "Res"))

; TODO no se si hará falta el wrap-defaults
(def app (wrap-defaults app-routes site-defaults))

; TODO reemplazar el println x logging
(defn start-web-server [port]
  (println (str "Starting web server on http://localhost:" port))
  (run-server app {:port port}))

(defstate web-server
  :start (start-web-server (env :port))
  :stop (web-server :timeout 100))
