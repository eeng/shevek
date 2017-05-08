(ns shevek.schemas.report
  (:require [schema.core :as s]))

(s/defschema SortBy
  {:name s/Str
   :descending s/Bool})

(s/defschema Split
  {:name s/Str
   :limit (s/cond-pre s/Int s/Str)
   (s/optional-key :sort-by) SortBy
   (s/optional-key :granularity) s/Str})

(s/defschema Filter
  {:name s/Str
   (s/optional-key :period) s/Str
   (s/optional-key :operator) s/Str
   (s/optional-key :value) [(s/maybe s/Str)]})

(s/defschema Pinboard
  {:measure s/Str :dimensions [Split]})

; TODO falta el user
(s/defschema Report
  {(s/optional-key :_id) s/Any
   (s/optional-key :name) s/Str ; Optional when restored from URL
   (s/optional-key :description) s/Str
   (s/optional-key :pin-in-dashboard) s/Bool ; Optional when restored from URL
   :cube s/Any
   :measures [s/Str]
   :filter [Filter]
   :split [Split]
   :pinboard Pinboard})
