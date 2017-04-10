(ns shevek.querying.schemas
  (:require [schema.core :as s]
            [shevek.schema.schemas :refer [Cube Dimension Measure]]))

(s/defschema SortBy
  {(s/optional-key :dimension) Dimension
   (s/optional-key :measure) Measure
   :descending s/Bool})

(s/defschema Split
  {:dimension Dimension
   :limit (s/cond-pre s/Int s/Str)
   (s/optional-key :sort-by) SortBy
   (s/optional-key :granularity) s/Str})

(s/defschema TimeFilter
  {:dimension Dimension
   :selected-period s/Keyword})

(s/defschema NormalFilter
  {:dimension Dimension
   :operator s/Str
   (s/optional-key :value) #{(s/maybe s/Str)}})

(s/defschema Filter
  (s/if :selected-period TimeFilter NormalFilter))

(s/defschema Pinboard
  {:measure Measure :dimensions [Split]})

(s/defschema Result {s/Keyword s/Any})

(s/defschema Viewer
  {:cube Cube
   (s/optional-key :filter) [(s/one TimeFilter "tf") NormalFilter]
   (s/optional-key :split) [Split]
   (s/optional-key :arrived-split) [Split]
   (s/optional-key :measures) [Measure]
   (s/optional-key :pinboard) Pinboard
   (s/optional-key :results) {(s/enum :main :pinboard :filter) (s/cond-pre [Result] {s/Str [Result]})}
   (s/optional-key :last-added-filter) s/Any})
