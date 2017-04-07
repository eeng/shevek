(ns shevek.schema.repository
  (:require [schema.core :as s]
            [monger.collection :as mc]))

(s/defschema NTD
  {:name s/Str
   :title s/Str
   (s/optional-key :description) (s/maybe s/Str)})

(s/defschema Dimension
  (assoc NTD :type s/Str))

(s/defschema Measure
  (assoc NTD :type (s/enum "count" "longSum" "doubleSum" "hyperUnique")))

(s/defschema Cube
  (assoc NTD
         :dimensions [Dimension]
         :measures [Measure]
         (s/optional-key :_id) s/Any))

(s/defn save-cube [db cube :- Cube]
  (mc/save-and-return db "cubes" cube))

(defn find-cubes [db]
  (mc/find-maps db "cubes"))

(defn find-cube [db name]
  (mc/find-one-as-map db "cubes" {:name name}))
