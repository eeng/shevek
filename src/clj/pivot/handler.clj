(ns pivot.handler
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources not-found]]
            [clojure.java.io :as io]))

(defroutes app-routes
  (GET "/" [] (-> "public/index.html" io/resource slurp))
  (POST "/rpc" {params :params} (do (println params) {:status 200 :body params}))
  (resources "/")
  (not-found (-> "public/404.html" io/resource slurp)))

; Para habilitar el anti-forgery habría que setearlo en una var en el index con (anti-forgery-field) y luego en los POST de cljs-ajax agregarlo al header X-CSRF-Token
(def app (-> app-routes
             (wrap-restful-format)
             (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
