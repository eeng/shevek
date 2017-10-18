(ns shevek.querying.auth
  (:require [shevek.schemas.query :refer [Query]]
            [shevek.lib.collections :refer [find-by]]
            [schema.core :as s]))

(s/defn filter-query [{:keys [allowed-cubes]} {:keys [cube] :as q} :- Query]
  (let [{:keys [filters]} (find-by :name cube allowed-cubes)]
    (update q :filters concat filters)))
