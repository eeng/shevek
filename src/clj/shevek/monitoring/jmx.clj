(ns shevek.monitoring.jmx
  (:require [clojure.java.jmx :as jmx]
            [mount.core :refer [defstate]]))

(def stats (atom {:Requests 0}))

(defn inc-requests []
  (swap! stats update :Requests inc))

(defn- register-mbeans []
  (jmx/register-mbean
    (jmx/create-bean stats)
    "shevek:type=Stats"))

(defn- unregister-mbeans []
  (jmx/unregister-mbean "shevek:type=Stats"))

(defstate mbeans
  :start (register-mbeans)
  :stop (unregister-mbeans))

#_(map #(.toString %) (jmx/mbean-names "*:*"))
#_(jmx/attribute-names "shevek:type=Stats")
#_(jmx/read "shevek:type=Stats" :Requests)
#_(do (unregister-mbeans) (register-mbeans))
