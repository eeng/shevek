(ns shevek.schema.repository
  (:require [schema.core :as s]
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
