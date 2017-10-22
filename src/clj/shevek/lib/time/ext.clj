(ns shevek.lib.time.ext
  (:require [clj-time.core :as t])
  (:import org.joda.time.DateTime org.joda.time.Period))

(defn plus-period [str-date str-period]
  (let [period (Period/parse str-period)
        date (DateTime/parse str-date)]
    (-> date (.plus period) .toString)))

(defn minus-period [str-date str-period]
  (let [period (Period/parse str-period)
        date (DateTime/parse str-date)]
    (-> date (.minus period) .toString)))
