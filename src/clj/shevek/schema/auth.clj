(ns shevek.schema.auth
  (:require [schema.core :as s]
            [shevek.lib.collections :refer [includes?]]))

(defn filter-visible-cubes [{:keys [allowed-cubes] :or {allowed-cubes "all"}} cubes]
  (if (= allowed-cubes "all")
    cubes
    (let [allowed-cubes (map :name allowed-cubes)]
      (filter #(includes? allowed-cubes (:name %)) cubes))))
