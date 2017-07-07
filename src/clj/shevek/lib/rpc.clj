(ns shevek.lib.rpc
  (:require [clojure.string :refer [split]]
            ; Hay que colocar las api aca para que las resuelva el call-fn en los tests de aceptación (y posiblemente luego tb en production)
            [shevek.reports.api]
            [shevek.querying.api]
            [shevek.users.api]
            [shevek.schema.api]))

(defn call-fn
  "Given a map like {:fn 'ns/func' :args [1 2]} calls (shevek.ns/func 1 2)"
  [{fid :fn args :args :or {args []}}]
  (let [[namespace-suffix fn-name] (split fid #"/")
        namespace (str "shevek." namespace-suffix)
        f (ns-resolve (symbol namespace) (symbol fn-name))]
    (if f
      (apply f args)
      (throw (IllegalArgumentException. (str "There is no remote function with fid " fid))))))

(defn controller [{:keys [params]}]
  {:status 200 :body (call-fn params)})
