(ns shevek.web.handler
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources]]
            [shevek.web.logging :refer [wrap-request-logging wrap-uuid]]
            [shevek.web.error :refer [wrap-server-error client-error]]
            [shevek.web.pages :as pages]
            [shevek.web.assets :refer [wrap-asset-pipeline]]
            [shevek.lib.transit-handlers :as th]
            [shevek.lib.rpc :as rpc]
            [shevek.lib.auth :as auth :refer [wrap-current-user]]
            [shevek.config :refer [config]]
            [shevek.monitoring.middleware :refer [wrap-stats]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [restrict error]]))

(defn should-be-authenticated
  "The default buddy handler checks for the presence of :identity in the request which is set by the backend.
   But that isn't enough to reject requests made by deleted users, for example."
  [request]
  (boolean (:user request)))

(defn not-authenticated [request _]
  {:status 401 :body "Not authenticated"})

(defn not-authorized [request _]
  {:status 403 :body "Not authorized"})

(defn not-found [request]
  {:status 404 :body "Not found"})

(defroutes app-routes
  (resources "/public")
  (GET "/bundles/*" [] not-found) ; Otherwise stacktrace-js throws an error when a source map doesn't exists (happens with react-grid-layout)
  (GET "/*" [] pages/index)
  (POST "/login" [] auth/login)
  (POST "/logout" [] auth/logout)
  (POST "/rpc" [] (restrict rpc/controller {:handler should-be-authenticated :on-error not-authenticated}))
  (POST "/error" [] (restrict client-error {:handler should-be-authenticated :on-error not-authenticated})))

(defn- wrap-session-refresh
  "This recreate the cookie on every request so the Expires field is updated.
   Otherwise the session could expire even if the user is using the application."
  [handler]
  (fn [{:keys [session] :as request}]
    (let [response (handler request)]
      (cond-> response
              (not (contains? response :session))
              (assoc :session (vary-meta session assoc :recreate true))))))

; This needs to be a defn a not a def because of the config which will only be available after mount/start
; The wrap-authorization needs to be on top of wrap-server-error so it catches the throw-unauthorized which in turn return the not-authorized response and then the wrap-server-error doesn't receive the exception (which would send the email that we don't want in that case)
(defn app []
  (let [backend (backends/session {:unauthorized-handler not-authorized})]
    (-> app-routes
        (wrap-authorization backend)
        (wrap-server-error)
        (wrap-stats)
        (wrap-request-logging)
        (wrap-current-user)
        (wrap-uuid)
        (wrap-authentication backend)
        (wrap-session-refresh)
        (wrap-session {:store (cookie-store {:key (config [:session :key])})
                       :cookie-attrs {:http-only true :same-site :lax :max-age (config [:session :timeout])}})
        (wrap-restful-format :response-options {:transit-json {:handlers th/write-handlers}})
        (wrap-asset-pipeline)
        (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false)))))
