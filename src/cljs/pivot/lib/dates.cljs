(ns pivot.lib.dates
  (:require [cljs-time.format :as f]
            [cljs-time.core :as t]
            [cljs-time.extend]))

;; TODO todas estas funciones van a trabajar con el timezone en UTC. Ver como influye eso al usar la local

(def parse-time f/parse)

(def now t/now)

(def yesterday t/yesterday)

(def beginning-of-day t/at-midnight)

(defn end-of-day [time]
  (-> (t/at-midnight time)
      (t/plus (t/days 1))
      (t/minus (t/millis 1))))

(def beginning-of-month t/first-day-of-the-month)
(def end-of-month (comp end-of-day t/last-day-of-the-month))

(defn to-iso8601 [time]
  (f/unparse (:date-time f/formatters) time))

; So the goog.date's are serialized as iso8601 strings with pr-str
(extend-protocol IPrintWithWriter
  goog.date.UtcDateTime
  (-pr-writer [obj writer opts]
    (-write writer "#inst ")
    (pr-writer (to-iso8601 obj) writer opts)))
