(ns shevek.engine.druid-native.planner.topn
  (:require [shevek.driver.druid :refer [send-query]]
            [shevek.engine.druid-native.planner.common :refer [dimension-spec dimension-order add-common-fields]]
            [shevek.engine.utils :refer [time-zone defaultLimit]]
            [shevek.domain.dimension :refer [sort-by-same?]]))

(defn- generate-metric-field [{:keys [sort-by] :as dim} measures]
  (let [descending (or (nil? (:descending sort-by)) (:descending sort-by))
        field (if (sort-by-same? dim)
                {:type "dimension" :ordering (dimension-order sort-by)}
                {:type "numeric" :metric (or (:name sort-by) (-> measures first :name))})]
    (if (or (and (sort-by-same? dim) (not descending))
            (and (not (sort-by-same? dim)) descending))
      field
      {:type "inverted" :metric field})))

(defn to-druid-query [{:keys [cube dimension measures filters] :as q}]
  (-> {:queryType "topN"
       :dataSource cube
       :dimension (dimension-spec dimension q)
       :metric (generate-metric-field dimension measures)
       :threshold (dimension :limit (or defaultLimit))
       :granularity "all"}
      (add-common-fields q)))

(defn from-druid-results [results]
  (-> results first :result))

(defn topn-query [driver q]
  (->> (to-druid-query q)
       (send-query driver)
       (from-druid-results)))
