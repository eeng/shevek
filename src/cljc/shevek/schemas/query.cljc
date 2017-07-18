(ns shevek.schemas.query
  (:require [schema.core :as s]))

(s/defschema Interval
  [(s/one s/Str "from") (s/one s/Str "to")])

(s/defschema TimeFilter
  {(s/optional-key :name) s/Str
   :interval Interval})

(s/defschema NormalFilter
  {:name s/Str
   :operator (s/enum "include" "exclude" "search" "is")
   :value (s/cond-pre s/Str #{(s/maybe s/Str)})})

(s/defschema Measure
  {:name s/Str
   :expression s/Str})

(s/defschema SortBy
  {:name s/Str
   :descending s/Bool
   (s/optional-key :expression) s/Str})

(s/defschema Split
  {:name s/Str
   (s/optional-key :limit) s/Int
   (s/optional-key :sort-by) SortBy
   (s/optional-key :granularity) s/Str})

(s/defschema Query
  {:cube s/Str
   :filter [(s/one TimeFilter "tf") NormalFilter]
   (s/optional-key :split) [Split]
   :measures [Measure]
   (s/optional-key :totals) s/Bool})

(s/defschema RawQuery
  {:cube s/Str
   :filter [(s/one TimeFilter "tf") NormalFilter]
   (s/optional-key :paging) {(s/optional-key :pagingIdentifiers) {s/Keyword s/Int}
                             :threshold s/Int}})
