(ns shevek.lib.transit-handlers
  (:require [cognitect.transit :as transit])
  (:import org.joda.time.ReadableInstant org.joda.time.DateTime))

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> ^ReadableInstant v .getMillis))
   (fn [v] (-> ^ReadableInstant v .getMillis .toString))))

(def write-handlers {DateTime joda-time-writer})
