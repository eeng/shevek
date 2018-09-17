(ns shevek.mailer
  (:require [postal.core :refer [send-message]]
            [shevek.config :refer [config]]
            [shevek.lib.collections :refer [assoc-nil]]
            [shevek.lib.logging :refer [pp-str]]
            [cuerdas.core :as str]))

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
            body (->> (dissoc data :message)
                      (map (fn [[k v]] (str (str/upper (name k)) ":\n" (pp-str v))))
                      (str/join "\n\n"))
            msg {:from from :to to :subject subject :body [{:type "text/plain; charset=utf-8" :content body}]}]
        (send-message server msg))))))

#_(let [exc (try (throw (Exception. "Foo")) (catch Exception e e))]
    @(notify-error exc {:request {:a "1" :b "2"}}))
