(ns shevek.domain.dw
  (:require [cuerdas.core :as str]
            [goog.string :as gstr]
            [shevek.lib.time.ext :refer [format-interval format-time-according-to-period]]
            [shevek.lib.number :as num]
            [shevek.lib.string :refer [format-bool regex-escape]]
            [shevek.i18n :refer [translation]]
            [shevek.domain.dimension :refer [time-dimension?]]
            [shevek.reflow.db :as db]))

(defn dimension-value [{:keys [name]} result]
  (->> name keyword (get result)))

(def measure-value dimension-value)

(defn format-dim-value [value {:keys [granularity type empty-value format]
                               :or {empty-value "Ø"}
                               :as dim}]
  (cond
    (empty? (str value)) empty-value
    (time-dimension? dim) (format-time-according-to-period value granularity)
    (= "BOOL" type) (format-bool value)
    :else (case format
            "dayOfWeekName" (get (translation :calendar :dayNames)
                                 (mod (str/parse-int value) 7)
                                 "dow not found")
            "monthName" (get (translation :calendar :months)
                             (dec (str/parse-int value))
                             "month not found")
            value)))

(defn totals-result? [result dim]
  (not (contains? result (-> dim :name keyword))))

(defn format-dimension [dim result]
  (when result
    (if (totals-result? result dim)
      "Total"
      (format-dim-value (dimension-value dim result) dim))))

(defn- personalize-abbreviations [format]
  (let [abbreviations (db/get-in [:settings :abbreviations])]
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
