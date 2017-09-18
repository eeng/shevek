(ns shevek.lib.rpc
  (:require [clojure.string :refer [split ends-with?]]
            [clojure.repl :refer [root-cause]]
            [taoensso.timbre :as log]
            [shevek.schema.api :as schema]
            [shevek.users.api :as users]
            [shevek.querying.api :as querying]
            [shevek.reports.api :as reports]
            [shevek.dashboards.api :as dashboards]))

(defn- api-fn? [f]
  (-> f meta :ns str (ends-with? ".api")))

(defn call-fn
  "Given a params map like {:fn 'ns-alias/func' :args [1 2]} calls (ns-alias/func 1 2). Make sure the alias is defined in the :require section."
  [{:keys [params] :as request}]
  (let [{fid :fn args :args :or {args []}} params
        f (ns-resolve 'shevek.lib.rpc (symbol fid))]
    (assert f (str "There is no remote function with fid " fid))
    (assert (api-fn? f) "Only api functions are allowed")
    (apply f request args)))

; The query runs within a pmap so if a timeout occours in Druid we will get an ExecutionException with the ExceptionInfo wrapped, hence the root-cause.
(defn controller [request]
  (try
    {:status 200 :body (call-fn request)}
    (catch clojure.lang.ExceptionInfo e
      (let [{:keys [type] :as data} (-> e root-cause ex-data)]
        (if (isa? type :shevek.app/error)
          (do
            (log/error e)
            {:status 599 :body data})
          (throw e))))))
