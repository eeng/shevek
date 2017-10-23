(ns shevek.schema.metadata
  (:require [shevek.config :refer [config]]
            [shevek.lib.druid-driver :as druid]
            [shevek.lib.time :refer [parse-time]]
            [shevek.lib.time.ext :refer [minus-period]]))

(defn cubes [dw]
  (druid/datasources dw))

(defn time-boundary [dw cube]
  (let [{:keys [minTime maxTime]} (-> (druid/send-query dw {:queryType "timeBoundary" :dataSource cube})
                                      first :result)]
    (assert maxTime (str "Cube " cube " not found"))
    {:min-time (parse-time minTime) :max-time (parse-time maxTime)}))

(defn- segment-metadata-query [dw cube max-time]
  (let [interval (str (minus-period max-time "P1M") "/" max-time)]
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

(defn cube-metadata [dw cube]
  (let [{:keys [max-time] :as tb} (time-boundary dw cube)
        {:keys [columns aggregators]} (first (segment-metadata-query dw cube max-time))
        dimensions (remove #(measure-column? % aggregators) (map with-name-inside columns))
        measures (map with-name-inside aggregators)]
    (merge {:dimensions dimensions :measures measures} tb)))

;; Examples

#_(cubes shevek.dw/dw)
#_(cube-metadata shevek.dw/dw "wikiticker")
