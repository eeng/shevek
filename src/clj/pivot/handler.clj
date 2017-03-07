(ns pivot.handler
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources not-found]]
            [clojure.java.io :as io]))

(defroutes app-routes
  (GET "/" [] "Res")
  (resources "/")
  (not-found (-> "404.html" io/resource slurp)))

; TODO no se si hará falta el wrap-defaults
(def app (wrap-defaults app-routes site-defaults))
