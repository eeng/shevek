(ns shevek.web.error
  (:require [shevek.mailer :refer [notify-error]]
            [taoensso.timbre :as log]
            [clojure.repl :refer [root-cause]]))

; Queries run within a pmap so if a timeout occours in Druid we will get an ExecutionException with the ExceptionInfo wrapped, hence the root-cause. That way we can translate some errors on the client.
(defn wrap-server-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (log/error e)
        (notify-error e {:request (dissoc req :user)})
        (let [message (or (-> e root-cause ex-data :error) (.getMessage e))]
          {:status 500 :body message})))))

(defn client-error [{:keys [params] :as req}]
  (notify-error (merge params {:request (dissoc req :params :body-params)}))
  {:status 200})
