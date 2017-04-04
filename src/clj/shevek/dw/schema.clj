(ns shevek.dw.schema
  (:require [schema.core :as s]
            [shevek.dw.engine :refer [cubes dimensions-and-measures]]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(s/defschema NTD
  {:name s/Str
   (s/optional-key :title) s/Str
   (s/optional-key :description) (s/maybe s/Str)})

(s/defschema Dimension
  (assoc NTD :type s/Str))

(s/defschema Measure
  (assoc NTD :type (s/enum "count" "longSum" "doubleSum" "hyperUnique")))

(s/defschema Cube
  (assoc NTD :dimensions [Dimension] :measures [Measure]))

(s/defn save-cube [db cube :- Cube]
  (mc/insert-and-return db "cubes" (assoc cube :_id (ObjectId.))))

(defn discover! [dw db]
  (doall
    (for [cube-name (cubes dw)]
      (let [[dimensions measures] (dimensions-and-measures dw cube-name)
            cube {:name cube-name
                  :dimensions dimensions
                  :measures measures}]
        (save-cube db cube)))))

#_(discover! shevek.dw.engine/dw (shevek.db/db))
