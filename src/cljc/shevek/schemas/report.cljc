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
   (s/optional-key :selected-period) s/Str
   (s/optional-key :operator) s/Str
   (s/optional-key :value) #{(s/maybe s/Str)}})

; TODO falta el pinboard y el user
(s/defschema Report
  {(s/optional-key :_id) s/Any
   :name s/Str
   (s/optional-key :description) s/Str
   :dashboard s/Bool
   :cube s/Any
   :measures [s/Str]
   :filter [Filter]
   :split [Split]})
