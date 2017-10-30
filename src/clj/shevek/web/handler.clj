(ns shevek.web.handler
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources not-found]]
            [clojure.java.io :as io]
            [shevek.web.logging :refer [wrap-request-logging]]
            [shevek.web.error :refer [wrap-server-error client-error]]
            [shevek.lib.transit-handlers :as th]
            [shevek.lib.rpc :as rpc]
            [shevek.lib.auth :as auth :refer [wrap-current-user]]
            [shevek.config :refer [config]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [restrict error]]))

(defn should-be-authenticated [request]
  (boolean (:user request)))

(defn not-authenticated [request _]
  {:status 401 :body {:error "Not authenticated"}})

(defn not-authorized [request _]
  {:status 403 :body {:error "Not authorized"}})

(defroutes app-routes
  (resources "/public")
  (GET "/*" [] (-> "public/index.html" io/resource slurp))
  (POST "/login" [] auth/controller)
  (POST "/rpc" [] (restrict rpc/controller {:handler should-be-authenticated :on-error not-authenticated}))
  (POST "/error" [] (restrict client-error {:handler should-be-authenticated :on-error not-authenticated})))

; This needs to be a defn a not a def because of the config which will only be available after mount/start
(defn app []
  (let [backend (backends/jws {:secret (config :jwt-secret) :unauthorized-handler not-authorized})]
    (-> app-routes
        (wrap-server-error)
        (wrap-request-logging)
        (wrap-current-user)
        (wrap-authentication backend)
        (wrap-authorization backend)
        (wrap-restful-format :response-options {:transit-json {:handlers th/write-handlers}})
        (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false)))))
