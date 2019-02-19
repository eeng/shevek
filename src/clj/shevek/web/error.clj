(ns shevek.web.error
  (:require [shevek.mailer :refer [notify-error]]
            [taoensso.timbre :as log]))

(defn wrap-server-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (log/error e)
        (notify-error e {:request (dissoc req :user :optimus-assets)})
        {:status 500 :body (.getMessage e)}))))

(defn client-error [{:keys [params] :as req}]
  (log/error "Client Error:" (:message params) "\n" (:stacktrace params))
  (notify-error (merge params {:request (dissoc req :params :body-params :optimus-assets)}))
  {:status 200 :body "Error Received"})
