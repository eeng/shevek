(ns shevek.lib.logging
  (:require [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [shevek.config :refer [env?]]))

(defn configure-logging! []
  (log/merge-config!
   (cond-> {:timestamp-opts {:pattern "yy-MM-dd HH:mm:ss.SSS"}} ; Por defecto no pone los msegs
           (env? :test) (assoc :appenders {:println {:enabled? false}
                                           :spit (appenders/spit-appender {:fname "log/test.log"})}))))
(defn pp-str [& args]
  (with-out-str (apply clojure.pprint/pprint args)))

(defmacro benchmark [str & body]
  `(let [start# (System/nanoTime)]
     ~@body
     (log/debug (format ~str (/ (- (System/nanoTime) start#) 1e6)))))
