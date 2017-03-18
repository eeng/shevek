(ns pivot.lib.dates
  (:require [cljs-time.format :as f]
            [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.extend]
            [clojure.string :as str]
            [pivot.i18n :refer [translation]]))

;; TODO todas estas funciones van a trabajar con el timezone en UTC. Ver como influye eso al usar la local

; El f/unparse es lentísimo supongo que xq prueba con varios formatos, pero aca por ahora solo parseamos iso8601 asi que con esto basta.
(defn parse-time [time]
  (c/from-long (.parse js/Date time)))

(def now t/now)

(def yesterday t/yesterday)

(def beginning-of-day t/at-midnight)

(defn end-of-day [time]
  (-> (t/at-midnight time)
      (t/plus (t/days 1))
      (t/minus (t/millis 1))))

(def beginning-of-month t/first-day-of-the-month)
(def end-of-month (comp end-of-day t/last-day-of-the-month))

(defn round-to-next-second [time]
  (if (zero? (t/milli time))
    time
    (t/date-time (t/year time) (t/month time) (t/day time) (t/hour time) (t/minute time) (inc (t/second time)))))

(defn to-iso8601 [time]
  (f/unparse (:date-time f/formatters) time))

; So the goog.date's are serialized as iso8601 strings with pr-str
(extend-protocol IPrintWithWriter
  goog.date.UtcDateTime
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (to-iso8601 obj) writer opts)))

(defn formatter [i18n-key]
  (f/formatter (translation :date-formats i18n-key)))

; TODO agregar formato week
(defn format-time-according-to-period [time period]
  (let [formatter (condp #(str/ends-with? %2 %1) period
                    "H" (formatter :hour)
                    "D" (formatter :day)
                    "M" (formatter :month))]
    (f/unparse formatter (parse-time time))))
