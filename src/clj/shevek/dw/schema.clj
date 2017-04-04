(ns shevek.dw.schema
  (:require [schema.core :as s]
            [shevek.dw.engine :refer [cubes dimensions-and-measures]]))

(s/defschema NTD
  {:name s/Str
   :title s/Str
   :description (s/maybe s/Str)})

(s/defschema Dimension
  (assoc NTD :type s/Str))

(s/defschema Measure
  (assoc NTD :type (s/enum "count" "longSum" "doubleSum" "hyperUnique")))

(s/defschema Cube
  (assoc NTD
         :dimensions [Dimension]
         :measures [Measure]))

(defn discover! [dw db]
  (for [cube-name (cubes dw)
        :let [[dimensions measures] (dimensions-and-measures dw cube-name)]]
    (let [cube {:name cube-name
                :dimensions dimensions
                :measures measures}]
      cube)))

#_(discover! shevek.dw.engine/dw nil)
