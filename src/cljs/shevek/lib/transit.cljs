(ns shevek.lib.transit
  (:require [ajax.core :as ajax]
            [cognitect.transit :as t]
            [goog.date.DateTime]))

(def write-handlers
  {:handlers
   {goog.date.DateTime
    (t/write-handler
     (constantly "m")
     (fn [v] (.getTime v))
     (fn [v] (str (.getTime v))))}})

(def transit-request-format (ajax/transit-request-format write-handlers))
