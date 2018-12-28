(ns shevek.monitoring.middleware
  (:require [shevek.monitoring.jmx :refer [inc-requests]]))

(defn wrap-stats [handler]
  (fn [request]
    (inc-requests)
    (handler request)))
