(ns shevek.schemas.query
  (:require [schema.core :as s]))

(s/defschema AbsoluteTimeFilter
  {(s/optional-key :name) s/Str
   :interval [(s/one s/Str "from") (s/one s/Str "to")]})

(s/defschema RelativeTimeFilter
  {(s/optional-key :name) s/Str
   :period s/Str})

(s/defschema TimeFilter
  (s/if :interval AbsoluteTimeFilter RelativeTimeFilter))

(s/defschema StringOrNumber
  (s/cond-pre s/Str s/Num))

(s/defschema NormalFilter
  {:name s/Str
   :operator (s/enum "include" "exclude" "search" "is")
   :value (s/cond-pre StringOrNumber [(s/maybe StringOrNumber)])})

(s/defschema Filter
  (s/if #(or (:period %) (:interval %)) TimeFilter NormalFilter))

(s/defschema Filters
  [(s/one TimeFilter "tf") NormalFilter])

(def Measure s/Str)

(s/defschema SortBy
  {:name s/Str
   :descending s/Bool})

(s/defschema Split
  {:name s/Str
   (s/optional-key :on) (s/enum "rows" "columns")
   (s/optional-key :limit) s/Num
   (s/optional-key :sort-by) SortBy
   (s/optional-key :granularity) s/Str})

(s/defschema Query
  {:cube s/Str
   :filters Filters
   (s/optional-key :splits) [Split]
   :measures [Measure]
   (s/optional-key :totals) s/Bool
   (s/optional-key :time-zone) s/Str})

(s/defschema Paging
  {(s/optional-key :pagingIdentifiers) {s/Keyword s/Int}
   (s/optional-key :threshold) s/Int})

(s/defschema RawQuery
  {:cube s/Str
   :filters Filters
   (s/optional-key :paging) Paging
   (s/optional-key :time-zone) s/Str})

(s/defschema Result {s/Keyword s/Any})
(s/defschema Results [Result])

(s/defschema RawQueryResults
  {:results Results :paging Paging})
