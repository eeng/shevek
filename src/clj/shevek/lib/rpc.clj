(ns shevek.lib.rpc
  (:require [clojure.string :refer [split ends-with?]]
            ; Hay que colocar las api aca para que las resuelva el call-fn en los tests de aceptación (y posiblemente luego tb en production)
            [shevek.reports.api]
            [shevek.querying.api]
            [shevek.users.api]
            [shevek.schema.api]))

(defn call-fn
  "Given a params map like {:fn 'ns.api/func' :args [1 2]} calls (shevek.ns.api/func 1 2)"
  [{:keys [params] :as request}]
  (let [{fid :fn args :args :or {args []}} params
        [namespace-suffix fn-name] (split fid #"/")
        namespace (str "shevek." namespace-suffix)
        f (ns-resolve (symbol namespace) (symbol fn-name))]
    (assert (ends-with? namespace-suffix ".api") "Naughty boy")
    (assert f (str "There is no remote function with fid " fid))
    (apply f request args)))

(defn controller [request]
  {:status 200 :body (call-fn request)})
