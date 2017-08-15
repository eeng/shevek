(ns shevek.lib.dates
  (:import java.time.ZonedDateTime java.time.Duration))

(defn plus-duration [str-date str-duration]
  (let [duration (Duration/parse str-duration)
        date (ZonedDateTime/parse str-date)]
    (-> date (.plus duration) .toString)))
