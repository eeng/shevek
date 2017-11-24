(ns shevek.lib.time.ext
  (:require [cljs-time.core :as t]
            [cljs-time.format :as f]
            [cljs-time.extend]
            [shevek.lib.dw.dims :refer [time-dimension]]
            [shevek.lib.period :refer [effective-interval to-interval]]
            [shevek.lib.time :refer [to-iso8601 parse-time]]
            [shevek.i18n :refer [translation]]
            [cuerdas.core :as str]))

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

(defn format-interval
  ([interval] (format-interval interval :day))
  ([interval formatter-key]
   (->> interval
        (map #(format-time % formatter-key))
        distinct
        (str/join " - "))))

(defn format-period [period max-time]
  (let [formatter-key (if (str/starts-with? period "latest") :minute :day)]
    (format-interval (to-interval period max-time) formatter-key)))

(defn default-granularity [{:keys [filters cube]}]
  (let [[from to] (effective-interval (time-dimension filters) (:max-time cube))
        span (t/in-days (t/interval from to))]
    (cond
      (<= span 7) "PT1H"
      (<= span 90) "P1D"
      :else "P1M")))
