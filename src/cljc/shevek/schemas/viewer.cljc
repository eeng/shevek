(ns shevek.schemas.viewer
  (:require [schema.core :as s]
            [shevek.schemas.cube :refer [Dimension Measure]]
            [shevek.schemas.query :refer [Result RawQueryResults]]))

(s/defschema DimensionSortBy
  (assoc Dimension :descending s/Bool))

(s/defschema MeasureSortBy
  (assoc Measure :descending s/Bool))

(s/defschema SortBy
  (s/if :expression MeasureSortBy DimensionSortBy))

(s/defschema Split
  (assoc Dimension
         :limit s/Int
         (s/optional-key :sort-by) SortBy
         (s/optional-key :granularity) s/Str))

(s/defschema TimeFilter
  (assoc Dimension
         :name (s/eq "__time")
         (s/optional-key :period) s/Keyword
         (s/optional-key :interval) [(s/one s/Any "from") (s/one s/Any "to")]))

(s/defschema NormalFilter
  (assoc Dimension
         :operator s/Str
         (s/optional-key :value) #{(s/maybe s/Str)}))

(s/defschema Filters
  [(s/one TimeFilter "tf") NormalFilter])

(s/defschema Pinboard
  {:measure Measure :dimensions [Split]})

; In the Viewer the Cube is slightly different from the schemas.cube.Cube: it starts only with the name and later receives a max-time
(s/defschema Cube
  {(s/optional-key :_id) s/Any
   :name s/Str
   (s/optional-key :title) s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :dimensions) [Dimension]
   (s/optional-key :measures) [Measure]
   (s/optional-key :max-time) s/Any})

(s/defschema Viewer
  {:cube Cube
   (s/optional-key :filter) Filters
   (s/optional-key :raw-data-filter) Filters
   (s/optional-key :split) [Split]
   (s/optional-key :arrived-split) [Split]
   (s/optional-key :measures) [Measure]
   (s/optional-key :pinboard) Pinboard
   (s/optional-key :results) {(s/optional-key :main) [Result]
                              (s/optional-key :filter) {s/Str [Result]}
                              (s/optional-key :pinboard) {s/Str [Result]}
                              (s/optional-key :raw) RawQueryResults}})
