(ns shevek.schema.manager
  (:require [shevek.schema.metadata :refer [cubes dimensions-and-measures]]
            [shevek.schema.repository :refer [save-cube]]))

(defn discover! [dw db]
  (doall
    (for [cube-name (cubes dw)]
      (let [[dimensions measures] (dimensions-and-measures dw cube-name)
            cube {:name cube-name
                  :dimensions dimensions
                  :measures measures}]
        (save-cube db cube)))))

#_(discover! shevek.dw2/dw (shevek.db/db))
