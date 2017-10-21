(ns shevek.lib.dw.time
  (:require [cljs-time.core :as t]
            [cljs-time.format :as f]
            [shevek.lib.dw.dims :refer [time-dimension]]
            [shevek.lib.period :refer [effective-interval to-interval]]
            [shevek.lib.dates :as d]
            [cuerdas.core :as str]))

(defn format-interval
  ([interval] (format-interval interval (d/formatter :day)))
  ([interval formatter]
   (->> interval
        (map #(f/unparse formatter %))
        distinct
        (str/join " - "))))

(defn format-period [period max-time]
  (let [formatter (d/formatter
                   (if (str/starts-with? period "latest") :minute :day))]
    (format-interval (to-interval period max-time) formatter)))

(defn default-granularity [{:keys [filters cube]}]
  (let [[from to] (effective-interval (time-dimension filters) (:max-time cube))
        span (t/in-days (t/interval from to))]
    (cond
      (<= span 7) "PT1H"
      (<= span 90) "P1D"
      :else "P1M")))
