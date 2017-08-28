(ns shevek.schema.auth
  (:require [schema.core :as s]
            [shevek.lib.collections :refer [includes?]]))

(s/defschema CubePermissions
  {:name s/Str})

(s/defschema Permissions
  (s/maybe {:allowed-cubes (s/cond-pre s/Str [CubePermissions])}))

(s/defn filter-visible-cubes [{:keys [allowed-cubes] :or {allowed-cubes "all"}} :- Permissions cubes]
  (if (= allowed-cubes "all")
    cubes
    (let [allowed-cubes (map :name allowed-cubes)]
      (filter #(includes? allowed-cubes (:name %)) cubes))))
