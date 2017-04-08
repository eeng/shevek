(ns shevek.schemas.app-db
  (:require [schema.core :as s])
  (:import goog.date.DateTime))

(s/defschema Settings
  {:lang s/Str})

(s/defschema Dimension
  {:name s/Str
   :title s/Str
   :type s/Str})

(s/defschema Measure
  {:name s/Str
   :title s/Str
   :type s/Str})

(s/defschema Cube
  {(s/optional-key :_id) s/Any
   :name s/Str
   (s/optional-key :title) s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :dimensions) [Dimension]
   (s/optional-key :measures) [Measure]
   (s/optional-key :max-time) goog.date.DateTime})

(s/defschema SortBy
  (assoc Dimension :descending s/Bool))

(s/defschema Split
  (assoc Dimension
         :limit (s/cond-pre s/Int s/Str)
         (s/optional-key :sort-by) SortBy
         (s/optional-key :granularity) s/Str))

(s/defschema TimeFilter
  (assoc Dimension
         :name (s/eq "__time")
         :selected-period s/Keyword))

(s/defschema NormalFilter
  (assoc Dimension
         :operator s/Str
         (s/optional-key :value) #{(s/maybe s/Str)}))

(s/defschema Filter
  (s/if :selected-period TimeFilter NormalFilter))

(s/defschema Pinboard
  {:measure Measure :dimensions [Split]})

(s/defschema Result {s/Keyword s/Any})

(s/defschema CubeView
  {:cube Cube
   (s/optional-key :filter) [(s/one TimeFilter "tf") NormalFilter]
   (s/optional-key :split) [Split]
   (s/optional-key :arrived-split) [Split]
   (s/optional-key :measures) [Measure]
   (s/optional-key :pinboard) Pinboard
   (s/optional-key :results) {(s/enum :main :pinboard :filter) (s/cond-pre [Result] {s/Str [Result]})}
   (s/optional-key :last-added-filter) s/Any})

(s/defschema AppDB
  {(s/optional-key :page) s/Keyword
   (s/optional-key :loading) {(s/cond-pre s/Keyword [s/Any]) s/Bool}
   (s/optional-key :cubes) {s/Str Cube}
   (s/optional-key :settings) Settings
   (s/optional-key :viewer) CubeView
   (s/optional-key :users) [s/Any]}) ; TODO
