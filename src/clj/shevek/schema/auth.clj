(ns shevek.schema.auth
  (:require [schema.core :as s]
            [shevek.lib.collections :refer [includes? find-by]]
            [shevek.lib.auth :refer [authorize]]))

(defn- cube-visible? [cube {:keys [allowed-cubes] :or {allowed-cubes "all"}}]
  (or (= allowed-cubes "all")
      (includes? (map :name allowed-cubes) (:name cube))))

(defn filter-cubes [user cubes]
  (filter #(cube-visible? % user) cubes))

(defn- measure-visible? [measure {:keys [measures] :or {measures "all"}}]
  (or (= measures "all")
      (includes? measures (:name measure))))

(defn filter-cube [{:keys [name measures] :as cube} {:keys [allowed-cubes] :as user}]
  (if (cube-visible? cube user)
    (let [allowed-cube (and (not= allowed-cubes "all") (find-by :name name allowed-cubes))]
      (update cube :measures (partial filterv #(measure-visible? % allowed-cube))))
    (select-keys cube [:name :title])))
