(ns shevek.engine.druid.metadata
  (:require [shevek.engine.druid.driver :refer [datasources send-query]]
            [shevek.lib.time :refer [parse-time]]
            [shevek.lib.time.ext :refer [minus-period]]))

(defn time-boundary [driver cube]
  (let [query {:queryType "timeBoundary" :dataSource cube}
        {:keys [minTime maxTime]} (-> (send-query driver query) first :result)]
    (assert maxTime (str "Cube " cube " not found"))
    {:min-time (parse-time minTime) :max-time (parse-time maxTime)}))

(defn- segment-metadata-query [driver cube max-time]
  (let [interval (str (minus-period max-time "P1M") "/" max-time)]
    (send-query
     driver
     {:queryType "segmentMetadata"
      :dataSource cube
      :merge true
      :analysisTypes ["aggregators"]
      :intervals [interval]
      :lenientAggregatorMerge true})))

(defn- measure-column? [{column :name} aggregators]
  (some #{(keyword column)} (keys aggregators)))

(defn only-used-keys [dim]
  (select-keys dim [:name :type]))

(defn- with-name-inside [[column fields]]
  (only-used-keys (merge fields {:name (name column)})))

(defn cube-metadata [driver cube]
  (let [{:keys [max-time] :as tb} (time-boundary driver cube)
        {:keys [columns aggregators]} (first (segment-metadata-query driver cube max-time))
        dimensions (remove #(measure-column? % aggregators) (map with-name-inside columns))
        measures (map with-name-inside aggregators)]
    (merge {:dimensions dimensions :measures measures} tb)))

; Examples

#_(cubes (shevek.engine.druid.driver/http-druid-driver "http://localhost:8082"))
