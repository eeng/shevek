(ns shevek.lib.logging
  (:require [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [shevek.config :refer [env? config]]
            [shevek.lib.collections :refer [assoc-if]]
            [clojure.string :as str])
  (:import java.util.TimeZone))

(defn output-fn
  "Customize the default output as we don't need the hostname, nor the timestamp on dev (cooper already provides one)"
  [data]
  (let [{:keys [level ?err #_vargs msg_ ?ns-str ?file timestamp_ ?line]} data]
    (str
     (when-not (env? :development) (force timestamp_) " ")
     (str/upper-case (first (name level)))  " "
     "[" (or ?ns-str ?file "?") ":" (or ?line "?") "] - "
     (force msg_)
     (when-let [err ?err]
       (str "\n" (log/stacktrace err nil))))))

(defn configure-logging! []
  (log/merge-config!
   (assoc-if (config :log)
             :output-fn output-fn
             :timestamp-opts {:pattern "yy-MM-dd HH:mm:ss.SSS" :timezone (TimeZone/getDefault)} ; By default msecs are missing and uses UTC
             :appenders (when (env? :test)
                          {:println {:enabled? false} :spit (appenders/spit-appender {:fname "log/test.log"})}))))

(defn pp-str [& args]
  (with-out-str (apply clojure.pprint/pprint args)))

(defmacro benchmark [str & body]
  `(let [start# (System/nanoTime)]
     ~@body
     (log/info (format ~str (/ (- (System/nanoTime) start#) 1e6)))))
