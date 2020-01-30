(ns shevek.domain.dw
  (:require [cuerdas.core :as str]
            [goog.string :as gstr]
            [shevek.lib.time.ext :refer [format-time-according-to-period]]
            [shevek.lib.number :as num]
            [shevek.lib.string :refer [format-bool]]
            [shevek.i18n :refer [translation]]
            [shevek.domain.dimension :refer [time-dimension?]]
            [shevek.reflow.db :as db]))

(defn dimension-value [{:keys [name]} result]
  (->> name keyword (get result)))

(def measure-value dimension-value)

(defn- format-dow [value]
  (get (translation :calendar :dayNames)
       (mod (str/parse-int value) 7)
       "dow not found"))

(defn- format-month [value]
  (get (translation :calendar :months)
       (dec (str/parse-int value))
       "month not found"))

(defn format-dim-value [value {:keys [granularity empty-value format]
                               :or {empty-value "Ø"}
                               :as dim}]
  (cond
    (empty? (str value)) empty-value
    (time-dimension? dim) (format-time-according-to-period value granularity)
    :else (case format
            "dayOfWeekName" (format-dow value)
            "monthName" (format-month value)
            "boolean" (format-bool value)
            value)))

(defn totals-result? [result dim]
  (not (contains? result (-> dim :name keyword))))

(defn format-dimension [dim result]
  (when result
    (if (totals-result? result dim)
      "Total"
      (format-dim-value (dimension-value dim result) dim))))

(defn- personalize-abbreviations [format]
  (let [abbreviations (db/get-in [:preferences :abbreviations])]
    (case abbreviations
      "yes" (str/replace format #"0$" "0a")
      "no" (str/replace format "a" "")
      format)))

(defn format-measure [{:keys [type format] :as dim} result]
  (when-let [value (measure-value dim result)]
    (if format
      (num/format value (personalize-abbreviations format))
      (condp = type
        "doubleSum" (gstr/format "%.2f" value)
        "hyperUnique" (gstr/format "%.0f" value)
        value))))
