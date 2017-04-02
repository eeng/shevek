(ns shevek.schemas.query
  (:require [schema.core :as s]
            [schema-tools.core :as st]
            [shevek.config :refer [env?]]))

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
   (s/optional-key :value) (s/cond-pre s/Str #{(s/maybe s/Str)})})

(s/defschema Split
  {:name s/Str
   (s/optional-key :limit) s/Int
   (s/optional-key :granularity) s/Str})

(s/defschema Measure
  {:name s/Str
   :type (s/enum "count" "longSum" "doubleSum" "hyperUnique")})

(s/defschema Query
  {:cube s/Str
   :filter [(s/one TimeFilter "tf") NormalFilter]
   (s/optional-key :split) [Split]
   :measures [Measure]
   (s/optional-key :totals) s/Bool})
