(ns shevek.schemas.cube
  (:require [schema.core :as s]))

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
