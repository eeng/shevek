(ns shevek.schemas.query
  (:require [schema.core :as s]))

(s/defschema SpecificTimeFilter
  {(s/optional-key :name) s/Str
   :interval [(s/one s/Str "from") (s/one s/Str "to")]})

(s/defschema RelativeTimeFilter
  {(s/optional-key :name) s/Str
   :selected-period s/Keyword})

(s/defschema TimeFilter
  (s/if :interval SpecificTimeFilter RelativeTimeFilter))

(s/defschema NormalFilter
  {:name s/Str
   :operator (s/enum "include" "exclude" "search" "is")
   :value (s/cond-pre s/Str #{(s/maybe s/Str)})})

(s/defschema Measure
  {:name s/Str
   :type (s/enum "count" "longSum" "doubleSum" "hyperUnique")})

(s/defschema SortBy
  {:name s/Str
   :type s/Str
   :descending s/Bool})

(s/defschema Split
  {:name s/Str
   (s/optional-key :limit) (s/cond-pre s/Int s/Str)
   (s/optional-key :sort-by) SortBy
   (s/optional-key :granularity) s/Str})

(s/defschema Query
  {:cube s/Str
   :filter [(s/one TimeFilter "tf") NormalFilter]
   (s/optional-key :split) [Split]
   :measures [Measure]
   (s/optional-key :totals) s/Bool})
