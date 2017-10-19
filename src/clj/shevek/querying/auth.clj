(ns shevek.querying.auth
  (:require [shevek.schemas.query :refer [Query]]
            [shevek.lib.collections :refer [find-by]]
            [shevek.schema.auth :refer [filter-measures]]
            [schema.core :as s]))

(s/defn filter-query [{:keys [allowed-cubes] :as user} {:keys [cube] :as q} :- Query]
  (let [{:keys [filters]} (find-by :name cube allowed-cubes)]
    (-> q
        (update :filters concat filters)
        (update :measures filter-measures cube user))))
