(ns shevek.schema.metadata
  (:require [shevek.config :refer [config]]
            [shevek.lib.druid-driver :as druid]
            [shevek.lib.time.ext :refer [minus-period]]
            [clojure.set :refer [rename-keys]]))

(defn cubes [dw]
  (druid/datasources dw))

(defn time-boundary [dw cube]
  (-> (druid/send-query dw {:queryType "timeBoundary" :dataSource cube})
      first :result
      (rename-keys {:maxTime :max-time :minTime :min-time})))

(defn- segment-metadata-query [dw cube]
  (let [{:keys [max-time]} (time-boundary dw cube)
        interval (str (minus-period max-time "P1M") "/" max-time)]
    (druid/send-query
     dw
     {:queryType "segmentMetadata"
      :dataSource cube
      :merge true
      :analysisTypes ["aggregators"]
      :intervals [interval]
      :lenientAggregatorMerge true})))

(defn- measure-column? [{column :name} aggregators]
  (some #{(keyword column)} (keys aggregators)))

(defn- with-name-inside [[column fields]]
  (merge (select-keys fields [:type])
         {:name (name column)}))

(defn dimensions-and-measures [dw cube]
  (let [{:keys [columns aggregators]} (first (segment-metadata-query dw cube))
        dimensions (remove #(measure-column? % aggregators) (map with-name-inside columns))
        measures (map with-name-inside aggregators)]
    [dimensions measures]))

;; Examples

#_(cubes shevek.dw/dw)
#_(dimensions-and-measures shevek.dw/dw "wikiticker")
#_(segment-metadata-query shevek.dw/dw "wikiticker")
#_(time-boundary shevek.dw/dw "wikiticker")
