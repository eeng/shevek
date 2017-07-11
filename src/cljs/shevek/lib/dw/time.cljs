(ns shevek.lib.dw.time
  (:require [cljs-time.core :as t]
            [cljs-time.format :as f]
            [shevek.lib.dates :as d :refer [parse-time]]
            [cuerdas.core :as str]
            [shevek.lib.dw.dims :refer [time-dimension]]))

(defn parse-max-time [max-time]
  (d/round-to-next-minute (parse-time max-time)))

(defn to-interval [period max-time]
  (let [now (d/now)
        max-time (or max-time now)
        day-of-last-week (t/minus (d/beginning-of-week now) (t/days 1))
        day-of-last-month (t/minus (d/beginning-of-month now) (t/days 1))
        day-of-last-quarter (t/minus (d/beginning-of-quarter now) (t/days 1))
        day-of-last-year (t/minus (d/beginning-of-year now) (t/days 1))]
    (case period
      :latest-hour [(t/minus max-time (t/hours 1)) max-time]
      :latest-day [(t/minus max-time (t/days 1)) max-time]
      :latest-7days [(t/minus max-time (t/days 7)) max-time]
      :latest-30days [(t/minus max-time (t/days 30)) max-time]
      :latest-90days [(t/minus max-time (t/days 90)) max-time]
      :current-day [(d/beginning-of-day now) (d/end-of-day now)]
      :current-week [(d/beginning-of-week now) (d/end-of-week now)]
      :current-month [(d/beginning-of-month now) (d/end-of-month now)]
      :current-quarter [(d/beginning-of-quarter now) (d/end-of-quarter now)]
      :current-year [(d/beginning-of-year now) (d/end-of-year now)]
      :previous-day [(d/beginning-of-day (d/yesterday)) (d/end-of-day (d/yesterday))]
      :previous-week [(d/beginning-of-week day-of-last-week) (d/end-of-week day-of-last-week)]
      :previous-month [(d/beginning-of-month day-of-last-month) (d/end-of-month day-of-last-month)]
      :previous-quarter [(d/beginning-of-quarter day-of-last-quarter) (d/end-of-quarter day-of-last-quarter)]
      :previous-year [(d/beginning-of-year day-of-last-year) (d/end-of-year day-of-last-year)])))

(defn format-interval
  ([interval] (format-interval interval (d/formatter :day)))
  ([interval formatter]
   (->> interval
        (map #(f/unparse formatter %))
        distinct
        (str/join " - "))))

(defn format-period [period max-time]
  (let [formatter (d/formatter
                   (if (str/starts-with? (name period) "latest") :minute :day))]
    (format-interval (to-interval period max-time) formatter)))

(defn effective-interval [{:keys [filter cube]}]
  (let [{:keys [period interval]} (time-dimension filter)
        interval (when interval [(first interval) (d/end-of-day (second interval))])]
    (if period (to-interval period (:max-time cube)) interval)))

(defn default-granularity [viewer]
  (let [[from to] (effective-interval viewer)
        span (t/in-days (t/interval from to))]
    (cond
      (<= span 7) "PT1H"
      (<= span 90) "P1D"
      :else "P1M")))
