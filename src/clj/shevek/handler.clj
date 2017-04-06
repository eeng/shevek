(ns shevek.handler
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources not-found]]
            [clojure.java.io :as io]
            [clojure.string :refer [split]]
            [shevek.logging :refer [wrap-request-logging]]
            [shevek.lib.transit-handlers :as th]))

(defn call-fn
  "Given a map like {:fn 'ns/func' :args [1 2]} calls (shevek.ns/func 1 2)"
  [{fid :fn args :args :or {args []}}]
  (let [[namespace-suffix fn-name] (split fid #"/")
        namespace (str "shevek." namespace-suffix)
        f (ns-resolve (symbol namespace) (symbol fn-name))]
    (if f
      (apply f args)
      (throw (IllegalArgumentException. (str "There is no remote function with fid " fid))))))

(defroutes app-routes
  (GET "/" [] (-> "public/index.html" io/resource slurp))
  (POST "/rpc" {params :params} {:status 200 :body (call-fn params)})
  (resources "/")
  (not-found (-> "public/404.html" io/resource slurp)))

; TODO Para habilitar el anti-forgery habría que setearlo en una var en el index con (anti-forgery-field) y luego en los POST de cljs-ajax agregarlo al header X-CSRF-Token
(def app (-> app-routes
             (wrap-request-logging)
             (wrap-restful-format :params-options {:transit-json {:handlers th/read-handlers}}
                                  :response-options {:transit-json {:handlers th/write-handlers}})
             (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
