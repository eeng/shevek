(ns shevek.schemas.designer
  (:require [schema.core :as s]
            [shevek.schemas.cube :refer [Dimension Measure]]
            [shevek.schemas.query :refer [Results RawQueryResults SortBy StringOrNumber]]
            [shevek.schemas.report :refer [Report]]))

(s/defschema Split
  (assoc Dimension
         :limit s/Int
         (s/optional-key :on) (s/enum "rows" "columns")
         (s/optional-key :sort-by) SortBy
         (s/optional-key :granularity) s/Str))

(s/defschema TimeFilter
  (assoc Dimension
         :name (s/eq "__time")
         (s/optional-key :period) s/Str
         (s/optional-key :interval) [(s/one s/Any "from") (s/one s/Any "to")]))

(s/defschema NormalFilter
  (assoc Dimension
         :operator s/Str
         (s/optional-key :value) #{(s/maybe StringOrNumber)}))

(s/defschema Filters
  [(s/one TimeFilter "tf") NormalFilter])

(s/defschema Pinboard
  {:measure Measure :dimensions [Split]})

(s/defschema Designer
  {:report Report
   (s/optional-key :on-report-change) s/Any
   (s/optional-key :viztype) s/Keyword
   (s/optional-key :filters) Filters
   (s/optional-key :splits) [Split]
   (s/optional-key :measures) [Measure]
   (s/optional-key :pinboard) Pinboard
   (s/optional-key :report-results) Results
   (s/optional-key :pinboard-results) {s/Str Results}
   (s/optional-key :maximized) s/Bool})
