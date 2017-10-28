(ns shevek.mailer
  (:require [postal.core :refer [send-message]]
            [shevek.config :refer [config]]
            [shevek.lib.collections :refer [assoc-nil]]
            [cuerdas.core :as str]
            [taoensso.timbre :as log]))

(defn notify-error [exception & [{:as data}]]
  (log/error exception)
  (future
   (when (config [:notifications :errors] false)
     (let [{:keys [server errors]} (config :notifications)
           hostname (.getHostName (java.net.InetAddress/getLocalHost))
           from (format "Shevek <shevek@%s>" hostname)
           subject (str "[ERROR] " (.getMessage exception))
           stacktrace (apply str (interpose "\n" (.getStackTrace exception)))
           body (->>
                 [(when data (str "DATA:\n" data))
                  (when stacktrace (str "STACKTRACE:\n" stacktrace))]
                 (remove nil?)
                 (str/join "\n\n"))
           msg (assoc-nil errors :from from :subject subject :body body)]
       (send-message server msg)))))

#_(let [exc (try (throw (Exception. "Foo")) (catch Exception e e))]
    @(notify-error exc))
