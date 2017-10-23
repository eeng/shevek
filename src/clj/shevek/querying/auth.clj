(ns shevek.querying.auth
  (:require [shevek.lib.collections :refer [find-by]]
            [shevek.schema.auth :refer [filter-measures]]))

(defn filter-query [{:keys [allowed-cubes] :as user} {:keys [cube measures] :as q}]
  (let [{:keys [filters]} (find-by :name cube allowed-cubes)]
    (cond-> (update q :filters concat filters)
            measures (update :measures filter-measures cube user))))
