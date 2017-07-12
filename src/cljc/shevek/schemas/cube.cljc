(ns shevek.schemas.cube
  (:require [schema.core :as s]))

(s/defschema NTD
  {:name (s/constrained s/Str #(> (count %) 1))
   :title s/Str
   (s/optional-key :description) (s/maybe s/Str)})

(s/defschema Dimension
  (assoc NTD :type s/Str))

(s/defschema Measure
  (assoc NTD
         (s/optional-key :type) s/Str
         :expression s/Str
         (s/optional-key :format) s/Str))

(s/defschema Cube
  (assoc NTD
         :dimensions [Dimension]
         :measures [Measure]
         (s/optional-key :_id) s/Any))
