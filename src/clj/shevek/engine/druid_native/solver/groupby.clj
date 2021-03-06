(ns shevek.engine.druid-native.solver.groupby
  (:require [shevek.driver.druid :refer [send-query]]
            [shevek.engine.druid-native.solver.common :refer [dimension-spec dimension-order add-common-fields]]
            [shevek.engine.utils :refer [time-zone defaultLimit]]))

(defn to-druid-query [{:keys [cube dimension] :as q}]
  (-> {:queryType "groupBy"
       :dataSource cube
       :dimensions [(dimension-spec (:sort-by dimension) q)
                    (dimension-spec dimension q)]
       :granularity "all"
       :limitSpec {:type "default"
                   :limit (or (dimension :limit) defaultLimit)
                   :columns [{:dimension (get-in dimension [:sort-by :name])
                              :direction (if (get-in dimension [:sort-by :descending] false)
                                           "descending"
                                           "ascending")
                              :dimensionOrder (dimension-order (get-in dimension [:sort-by]))}]}}
      (add-common-fields q)))

(defn from-druid-results [results]
  (map :event results))

(defn groupby-query [driver q]
  (->> (to-druid-query q)
       (send-query driver)
       (from-druid-results)))
