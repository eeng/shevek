(ns pivot.handler
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [files not-found]]))

(defroutes app-routes
  (GET "/" [] "Res"))

; TODO no se si hará falta el wrap-defaults
(def app (wrap-defaults app-routes site-defaults))
