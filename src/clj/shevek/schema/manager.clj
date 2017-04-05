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

(defn- update-cube [new old]
  (or old new))

(defn discover! [dw db]
  (let [existing-cubes (find-cubes db)
        discovered-cubes (discover-cubes dw)]
    (doall
      (for [dc discovered-cubes]
        (save-cube db (->> existing-cubes
                           (detect #(same-name? dc %))
                           (update-cube dc)))))))

#_(discover! shevek.dw2/dw (shevek.db/db))
