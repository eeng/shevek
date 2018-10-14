(ns shevek.engine.druid-native.planner.timeseries
  (:require [shevek.driver.druid :refer [send-query]]
            [shevek.engine.druid-native.planner.common :refer [time-zone add-common-fields]]))

(defn to-druid-query [{:keys [cube dimension] :as q}]
  (-> {:queryType "timeseries"
       :dataSource cube
       :granularity (if dimension
                      {:type "period" :period (:granularity dimension) :timeZone (time-zone q)}
                      "all")
       :descending (get-in dimension [:sort-by :descending] false)
       :context {:skipEmptyBuckets true}}
      (add-common-fields q)))

(defn from-druid-results [{:keys [dimension]} results]
  (cond->> results
    (:limit dimension) (take (:limit dimension))
    true (map (fn [{:keys [result timestamp]}]
                (if dimension
                  (assoc result (keyword (:name dimension)) timestamp)
                  result)))))

(defn timeseries-query [driver q]
  (->> (to-druid-query q)
       (send-query driver)
       (from-druid-results q)))
