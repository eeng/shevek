(ns shevek.schema.auth
  (:require [shevek.lib.collections :refer [includes? find-by]]))

(defn- cube-visible? [cube-name {:keys [allowed-cubes] :or {allowed-cubes "all"}}]
  (or (= allowed-cubes "all")
      (includes? (map :name allowed-cubes) cube-name)))

(defn- measure-visible? [measure {:keys [measures] :or {measures "all"}}]
  (let [measure (if (string? measure) measure (:name measure))]
    (or (= measures "all")
        (includes? measures measure)
        (= measure "rowCount"))))

(defn filter-measures [measures cube-name {:keys [allowed-cubes] :as user}]
  (let [allowed-cube (cond
                       (not (cube-visible? cube-name user)) {:measures []}
                       (= allowed-cubes "all") {:measures "all"}
                       :else (find-by :name cube-name allowed-cubes))]
    (filterv #(measure-visible? % allowed-cube) measures)))

(defn filter-cube [{:keys [name] :as cube} user]
  (update cube :measures filter-measures name user))

(defn filter-cubes [user cubes]
  (->> cubes
    (filter #(cube-visible? (:name %) user))
    (map #(filter-cube % user))))
