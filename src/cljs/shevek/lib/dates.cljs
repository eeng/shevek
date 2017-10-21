(ns shevek.lib.dates
  (:require [cljs-time.format :as f]
            [cljs-time.extend]
            [shevek.lib.time :refer [to-iso8601 parse-time]]
            [shevek.i18n :refer [translation]]))

; So the goog.date's are serialized as iso8601 strings with pr-str
(extend-protocol IPrintWithWriter
  goog.date.UtcDateTime
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (to-iso8601 obj) writer opts)))

(defn formatter [i18n-key]
  (f/formatter (translation :date-formats i18n-key)))

(defn format-time-according-to-period [time period]
  (let [formatter (condp re-find period
                    #"PT(\d+)M" (formatter :minute)
                    #"PT(\d+)H" (formatter :hour)
                    #"P(\d+)D" (formatter :day)
                    #"P(\d+)W" (formatter :day)
                    #"P(\d+)M" (formatter :month))]
    (->> time parse-time (f/unparse formatter))))

(defn format-date [time]
  (f/unparse (:date f/formatters) time))

(defn format-time
  ([time] (format-time time :second))
  ([time formatter-i18n-key]
   (if (instance? goog.date.Date time)
     (f/unparse (formatter formatter-i18n-key) time)
     (when time (-> time parse-time (format-time formatter-i18n-key))))))
