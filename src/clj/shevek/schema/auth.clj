(ns shevek.schema.auth
  (:require [schema.core :as s]
            [shevek.lib.collections :refer [includes?]]
            [shevek.schemas.user :refer [Permissions]]))

(s/defn filter-visible-cubes [{:keys [allowed-cubes] :or {allowed-cubes "all"}} :- Permissions cubes]
  (if (= allowed-cubes "all")
    cubes
    (let [allowed-cubes (map :name allowed-cubes)]
      (filter #(includes? allowed-cubes (:name %)) cubes))))
