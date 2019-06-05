(ns shevek.lib.logging
  (:require [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [shevek.config :refer [config]]
            [shevek.env :refer [env]]
            [shevek.lib.collections :refer [assoc-if]]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [mount.core :refer [defstate]])
  (:import java.util.TimeZone))

(defn output-fn
  "Customize the default output as we don't need the hostname, nor the timestamp on dev (cooper already provides one)"
  [data]
  (let [{:keys [level ?err #_vargs msg_ ?ns-str ?file timestamp_ ?line]} data]
    (str
     (when (config [:log :timestamp])
       (str (force timestamp_) " "))
     (str/upper-case (first (name level)))  " "
     "[" (or ?ns-str ?file "?") ":" (or ?line "?") "] - "
     (force msg_)
     (when-let [err ?err]
       (str "\n" (log/stacktrace err nil))))))

(defstate logger
  :start
  (do
    (log/merge-config!
     (assoc-if {:level (config [:log :level])}
               :output-fn output-fn
               :timestamp-opts {:pattern "yy-MM-dd HH:mm:ss.SSS" :timezone (TimeZone/getDefault)} ; By default msecs are missing and uses UTC
               :appenders (when (not= "stdout" (config [:log :to]))
                            {:println {:enabled? false} :spit (appenders/spit-appender {:fname (config [:log :to])})})))
    (log/info "Starting Shevek in" (env) "environment")
    :ok))

(defn pp-str [& args]
  (with-out-str (apply pprint args)))

(defmacro benchmark [{:keys [before after log-level] :or {log-level :info}} & body]
  (let [log-fn (case log-level
                 :info 'taoensso.timbre/info
                 :debug 'taoensso.timbre/debug)]
    `(let [start# (System/nanoTime)
           _# (when ~before (~log-fn ~before))
           result# (do ~@body)]
       (~log-fn (format ~after (/ (- (System/nanoTime) start#) 1e6)))
       result#)))
