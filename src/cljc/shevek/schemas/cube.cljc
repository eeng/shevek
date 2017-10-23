(ns shevek.schemas.cube
  (:require [schema.core :as s]))

(s/defschema NTD
  {:name (s/constrained s/Str #(> (count %) 1))
   :title s/Str
   (s/optional-key :description) (s/maybe s/Str)})

(s/defschema ExtractionFn
  {s/Keyword s/Str})

(s/defschema Dimension
  (assoc NTD
         :type s/Str
         (s/optional-key :column) s/Str
         (s/optional-key :extraction) [ExtractionFn]))

(s/defschema Measure
  (assoc NTD
         (s/optional-key :type) s/Str
         :expression s/Str
         (s/optional-key :format) s/Str
         (s/optional-key :favorite) s/Bool))

(s/defschema Cube
  (assoc NTD
         :dimensions [Dimension]
         :measures [Measure]
         (s/optional-key :id) s/Str
         (s/optional-key :default-time-zone) s/Str
         (s/optional-key :created-at) s/Any
         (s/optional-key :updated-at) s/Any
         (s/optional-key :min-time) s/Any
         (s/optional-key :max-time) s/Any))
