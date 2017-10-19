(ns shevek.lib.period
  (:require [shevek.lib.time :as t]))

(defn to-interval [period max-time]
  (let [now (t/now)
        max-time (t/parse-time max-time)
        max-time (t/round-to-next-minute (or max-time now))
        day-of-last-week (t/minus (t/beginning-of-week now) (t/days 1))
        day-of-last-month (t/minus (t/beginning-of-month now) (t/days 1))
        day-of-last-quarter (t/minus (t/beginning-of-quarter now) (t/days 1))
        day-of-last-year (t/minus (t/beginning-of-year now) (t/days 1))]
    (case period
      "latest-hour" [(t/minus max-time (t/hours 1)) max-time]
      "latest-day" [(t/minus max-time (t/days 1)) max-time]
      "latest-7days" [(t/minus max-time (t/days 7)) max-time]
      "latest-30days" [(t/minus max-time (t/days 30)) max-time]
      "latest-90days" [(t/minus max-time (t/days 90)) max-time]
      "current-day" [(t/beginning-of-day now) (t/end-of-day now)]
      "current-week" [(t/beginning-of-week now) (t/end-of-week now)]
      "current-month" [(t/beginning-of-month now) (t/end-of-month now)]
      "current-quarter" [(t/beginning-of-quarter now) (t/end-of-quarter now)]
      "current-year" [(t/beginning-of-year now) (t/end-of-year now)]
      "previous-day" [(t/beginning-of-day (t/yesterday)) (t/end-of-day (t/yesterday))]
      "previous-week" [(t/beginning-of-week day-of-last-week) (t/end-of-week day-of-last-week)]
      "previous-month" [(t/beginning-of-month day-of-last-month) (t/end-of-month day-of-last-month)]
      "previous-quarter" [(t/beginning-of-quarter day-of-last-quarter) (t/end-of-quarter day-of-last-quarter)]
      "previous-year" [(t/beginning-of-year day-of-last-year) (t/end-of-year day-of-last-year)])))

(defn normalize-interval [[from to]]
  [(t/parse-time from) (t/end-of-day (t/parse-time to))])
