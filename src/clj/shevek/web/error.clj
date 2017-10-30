(ns shevek.web.error
  (:require [shevek.mailer :refer [notify-error]]))

(defn wrap-server-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (notify-error e {:request (dissoc req :user)})
        (throw e)))))

(defn client-error [{:keys [params] :as req}]
  (notify-error (merge params {:request (dissoc req :params :body-params)}))
  {:status 200})
