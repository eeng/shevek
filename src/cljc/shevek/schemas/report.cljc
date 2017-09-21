(ns shevek.schemas.report
  (:require [schema.core :as s]))

(s/defschema SortBy
  {:name s/Str
   :descending s/Bool})

(s/defschema Split
  {:name s/Str
   :limit s/Num
   (s/optional-key :sort-by) SortBy
   (s/optional-key :granularity) s/Str})

(s/defschema Filter
  {:name s/Str
   (s/optional-key :period) s/Str
   (s/optional-key :interval) [s/Str]
   (s/optional-key :operator) s/Str
   (s/optional-key :value) [(s/maybe s/Str)]})

(s/defschema Pinboard
  {:measure s/Str :dimensions [Split]})

(s/defschema Report
  {(s/optional-key :id) s/Str
   (s/optional-key :name) s/Str ; Optional when restored from URL
   (s/optional-key :description) s/Str
   :cube s/Str
   :viztype s/Str
   :measures [s/Str]
   :filters [Filter]
   :row-splits [Split]
   :column-splits [Split]
   :pinboard Pinboard
   :user-id s/Str
   (s/optional-key :dashboards-ids) [s/Str]
   (s/optional-key :created-at) s/Any
   (s/optional-key :updated-at) s/Any})
