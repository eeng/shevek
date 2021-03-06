(ns shevek.engine.druid-native.solver
  (:require [shevek.engine.druid-native.solver.timeseries :refer [timeseries-query]]
            [shevek.engine.druid-native.solver.topn :refer [topn-query]]
            [shevek.engine.druid-native.solver.groupby :refer [groupby-query]]
            [shevek.domain.dimension :refer [time-dimension? sort-by-other-dimension?]]))

(defn resolve-expanded-query [driver {:keys [dimension] :as q}]
  (let [query-fn
        (cond
          (or (not dimension) (time-dimension? dimension)) timeseries-query
          (sort-by-other-dimension? dimension) groupby-query
          :else topn-query)]
    (query-fn driver q)))
