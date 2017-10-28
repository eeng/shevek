(ns shevek.web.error
  (:require [shevek.mailer :refer [notify-error]]
            [shevek.lib.logging :refer [pp-str]]))

(defn wrap-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (notify-error e (pp-str (dissoc req :user)))
        (throw e)))))
