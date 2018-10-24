(ns shevek.mailer
  (:require [postal.core :refer [send-message]]
            [shevek.config :refer [config]]
            [shevek.lib.logging :refer [pp-str]]
            [cuerdas.core :as str]))

(defn- pretty-print-key-and-value [[k v]]
  (str (str/upper (name k))
       ":\n"
       (if (string? v) v (pp-str v))))

(defn- format-body [record]
  (->> record
       (map pretty-print-key-and-value)
       (str/join "\n\n")))

(defn notify-error
  ([exception data]
   (let [message (.getMessage exception)
         stacktrace (apply str (interpose "\n" (.getStackTrace exception)))]
     (notify-error (assoc data :message message :stacktrace stacktrace))))
  ([{:keys [message] :as data}]
   (future
     (when (seq (config [:notifications :errors :to]))
       (let [{:keys [server errors]} (config :notifications)
             hostname (.getHostName (java.net.InetAddress/getLocalHost))
             from (format "Shevek <shevek@%s>" hostname)
             to (str/split (:to errors) ",")
             subject (str "[ERROR] " message)
             body (format-body (dissoc data :message))
             msg {:from from :to to :subject subject :body [{:type "text/plain; charset=utf-8" :content body}]}]
         (send-message server msg))))))

#_(let [exc (try (throw (Exception. "Foo")) (catch Exception e e))]
    @(notify-error exc {:request {:a "1" :b "2"}}))
