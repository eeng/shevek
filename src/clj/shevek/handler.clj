(ns shevek.handler
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources not-found]]
            [clojure.java.io :as io]
            [shevek.logging :refer [wrap-request-logging]]
            [shevek.lib.transit-handlers :as th]
            [shevek.lib.rpc :as rpc]
            [shevek.lib.auth :as auth]
            ; Hay que colocar las api aca para que las resuelva el call-fn en los tests de aceptación (y posiblemente luego tb en production)
            [shevek.reports.api]
            [shevek.querying.api]
            [shevek.users.api]
            [shevek.schema.api]))

(defroutes app-routes
  (GET "/" [] (-> "public/index.html" io/resource slurp))
  (POST "/login" {params :params} (auth/login params))
  (POST "/rpc" {params :params} {:status 200 :body (rpc/call-fn params)})
  (resources "/")
  (not-found (-> "public/404.html" io/resource slurp)))

; TODO Para habilitar el anti-forgery habría que setearlo en una var en el index con (anti-forgery-field) y luego en los POST de cljs-ajax agregarlo al header X-CSRF-Token
(def app (-> app-routes
             (wrap-request-logging)
             (wrap-restful-format :params-options {:transit-json {:handlers th/read-handlers}}
                                  :response-options {:transit-json {:handlers th/write-handlers}})
             (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
