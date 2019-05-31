(ns shevek.lib.rpc
  (:require [clojure.string :refer [split ends-with?]]
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

; I remove the optimus-assets otherwise when a schema validation error occours in any api method it would become very slow when the error middleware logs it
(defn controller [request]
  (try
    {:status 200 :body (call-fn (dissoc request :optimus-assets))}
    (catch clojure.lang.ExceptionInfo e
      (case (-> e ex-data :type)
        :shevek/record-not-found {:status 404 :body "Record not found"}
        (throw e)))))

#_(controller {:params {:fn "reports/find-all"} :user-id "5cec63d1f8d5029a718afe17"})
#_(controller {:params {:fn "dashboards/find-by-id" :args ["..."]}})
