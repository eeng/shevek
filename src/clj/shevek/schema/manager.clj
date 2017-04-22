(ns shevek.schema.manager
  (:require [shevek.schema.metadata :refer [cubes dimensions-and-measures]]
            [shevek.schema.repository :refer [save-cube find-cubes]]
            [shevek.lib.collections :refer [detect]]
            [cuerdas.core :as str]))

(defn- discover-cubes [dw]
  (for [cube-name (cubes dw)]
    (let [[dimensions measures] (dimensions-and-measures dw cube-name)]
      {:name cube-name :dimensions dimensions :measures measures})))

(defn- same-name? [{n1 :name} {n2 :name}]
  (= n1 n2))

(defn- corresponding [field coll]
  (detect #(same-name? field %) coll))

(defn- merge-dimensions [old-coll new-coll]
  (let [old-updated-fields (map #(merge % (corresponding % new-coll)) old-coll)
        new-fields (remove #(corresponding % old-coll) new-coll)]
    (concat old-updated-fields new-fields)))

(defn- set-default-title [{:keys [name title] :or {title (str/title name)} :as record}]
  (assoc record :title title))

(defn- set-default-expression [{:keys [type name] :as measure}]
  (let [type->agg-fn #(if (= % "hyperUnique") "count-distinct" "sum")]
    (assoc measure :expression (str "(" (type->agg-fn type) " $" name ")"))))

(defn set-default-titles [{:keys [dimensions measures] :as cube}]
  (-> (set-default-title cube)
      (assoc :dimensions (mapv set-default-title dimensions))
      (assoc :measures (mapv (comp set-default-expression set-default-title) measures))))

(defn- update-cube [old new]
  (-> (merge old (dissoc new :dimensions :measures))
      (assoc :dimensions (merge-dimensions (:dimensions old) (:dimensions new)))
      (assoc :measures (merge-dimensions (:measures old) (:measures new)))
      set-default-titles))

(defn discover! [dw db]
  (let [existing-cubes (find-cubes db)
        discovered-cubes (discover-cubes dw)]
    (doall
      (for [dc discovered-cubes]
        (save-cube db (update-cube (corresponding dc existing-cubes) dc))))))

#_(discover! shevek.dw/dw shevek.db/db)
