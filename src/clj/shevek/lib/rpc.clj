(ns shevek.lib.rpc
  (:require [clojure.string :refer [split]]))

(defn call-fn
  "Given a map like {:fn 'ns/func' :args [1 2]} calls (shevek.ns/func 1 2)"
  [{fid :fn args :args :or {args []}}]
  (let [[namespace-suffix fn-name] (split fid #"/")
        namespace (str "shevek." namespace-suffix)
        f (ns-resolve (symbol namespace) (symbol fn-name))]
    (if f
      (apply f args)
      (throw (IllegalArgumentException. (str "There is no remote function with fid " fid))))))
