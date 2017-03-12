(ns pivot.handler
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources not-found]]
            [clojure.java.io :as io]
            [clojure.string :refer [split]]))

(defn get-cubes []
  [{:name "vtol_stats" :title "VTOL Stats" :description "Estadísticas de uso del sistema VTOL."}
   {:name "eventos_pedidos" :title "Eventos de Pedidos" :description "Info sobre eventos generados por el ruteo de pedidos."}
   {:name "facturacion"}])

(defn get-dimensions [cube]
  (condp = cube
    "vtol_stats" [{:name "controller" :title "Controller" :cardinality 123}
                  {:name "action" :title "Action" :cardinality 43}]
    "eventos_pedidos" [{:name "evento"}
                       {:name "oficina"}
                       {:name "adicion" :type "LONG"}]
    "facturacion" [{:name "__time"}]))

(defn get-measures [cube]
  (condp = cube
    "vtol_stats" [{:name "requests"}
                  {:name "duration"}]
    "eventos_pedidos" [{:name "pedidos"}
                       {:name "usuarios"}]
    "facturacion" []))

(defn call-fn
  "Given a map like {:fn 'ns/func' :args [1 2]} calls (pivot.ns/func 1 2)"
  [{fid :fn args :args :or {args []}}]
  (println "Calling fn" fid "with args" args)
  (let [[namespace-suffix fn-name] (split fid #"/")
        namespace (str "pivot." namespace-suffix)
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
             (wrap-restful-format)
             (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
