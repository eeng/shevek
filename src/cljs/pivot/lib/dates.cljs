(ns pivot.lib.dates
  (:require [cljs-time.format :as f]
            [cljs-time.core :as t]
            [cljs-time.extend]))

; So the goog.date's are serialized as iso8601 strings
(extend-protocol IPrintWithWriter
  goog.date.UtcDateTime
  (-pr-writer [obj writer opts]
    (pr-writer (f/unparse (:date-time f/formatters) obj) writer opts)))

;; TODO todas estas funciones van a trabajar con el timezone en UTC. Ver como influye eso al usar la local

(def parse-time f/parse)

(def today t/now)

(def yesterday t/yesterday)

(def beginning-of-day t/at-midnight)

(defn end-of-day [time]
  (-> (t/at-midnight time)
      (t/plus (t/days 1))
      (t/minus (t/millis 1))))

; TODO hacer testing de esto
(defn- day-period [time]
  [(beginning-of-day time) (end-of-day time)])
