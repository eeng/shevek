(ns shevek.schema.manager
  (:require [shevek.schema.metadata :refer [cubes dimensions-and-measures]]
            [shevek.schema.repository :refer [save-cube find-cubes]]
            [shevek.lib.collections :refer [detect]]))

(defn- discover-cubes [dw]
  (for [cube-name (cubes dw)]
    (let [[dimensions measures] (dimensions-and-measures dw cube-name)]
      {:name cube-name :dimensions dimensions :measures measures})))

(defn- same-name? [{n1 :name} {n2 :name}]
  (= n1 n2))

(defn- corresponding [field coll]
  (detect #(same-name? field %) coll))

(defn- merge-fields [old-coll new-coll]
  (let [old-updated-fields (map #(merge % (corresponding % new-coll)) old-coll)
        new-fields (remove #(corresponding % old-coll) new-coll)]
    (concat old-updated-fields new-fields)))

(defn- update-cube [old new]
  (-> (merge old (dissoc new :dimensions :measures))
      (assoc :dimensions (merge-fields (:dimensions old) (:dimensions new)))
      (assoc :measures (merge-fields (:measures old) (:measures new)))))

(defn discover! [dw db]
  (let [existing-cubes (find-cubes db)
        discovered-cubes (discover-cubes dw)]
    (doall
      (for [dc discovered-cubes]
        (save-cube db (update-cube (corresponding dc existing-cubes) dc))))))

#_(discover! shevek.dw2/dw (shevek.db/db))
