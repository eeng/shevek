(ns shevek.schema.metadata
  (:require [shevek.config :refer [config]]
            [shevek.lib.druid-driver :as druid]
            [clojure.set :refer [rename-keys]]))

(defn cubes [dw]
  (druid/datasources dw))

(defn- segment-metadata-query [dw cube]
  (druid/send-query
   dw
   {:queryType "segmentMetadata"
    :dataSource {:type "table" :name cube}
    :merge true
    :analysisTypes ["aggregators"]
    :intervals ["2000/2100"]
    :lenientAggregatorMerge true}))

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

(defn time-boundary [dw cube]
  (-> (druid/send-query dw {:queryType "timeBoundary" :dataSource cube})
      first :result
      (rename-keys {:maxTime :max-time :minTime :min-time})))

;; Examples

(require '[shevek.dw2 :refer [dw]])
#_(cubes dw)
#_(dimensions-and-measures dw "wikiticker")
#_(segment-metadata-query dw "wikiticker")
#_(time-boundary dw "wikiticker")
