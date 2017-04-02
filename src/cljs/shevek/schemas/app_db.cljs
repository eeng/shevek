(ns shevek.schemas.app-db
  (:require [schema.core :as s])
  (:import goog.date.DateTime))

(def Settings
  {:lang s/Str})

(def Dimension
  {:name s/Str
   :title s/Str
   :type s/Str
   :cardinality (s/maybe s/Int)})

(def Measure
  {:name s/Str
   :title s/Str
   :type s/Str})

(def Cube
  {:name s/Str
   :title s/Str
   (s/optional-key :dimensions) [Dimension]
   (s/optional-key :measures) [Measure]
   (s/optional-key :max-time) goog.date.DateTime})

(def SortBy
  (s/if #(contains? % :cardinality)
        (assoc Dimension :descending s/Bool)
        (assoc Measure :descending s/Bool)))

(def Split
  (assoc Dimension
         :limit (s/cond-pre s/Int s/Str)
         (s/optional-key :sort-by) SortBy
         (s/optional-key :granularity) s/Str))

(def TimeFilter
  (assoc Dimension
         :name (s/eq "__time")
         :selected-period s/Keyword))

(def NormalFilter
  (assoc Dimension
         :operator s/Str
         (s/optional-key :value) #{(s/maybe s/Str)}))

(def Filter
  (s/if :selected-period TimeFilter NormalFilter))

(def Pinboard
  {:measure Measure :dimensions [Split]})

(def Result {s/Keyword s/Any})

(def CubeView
  {:cube s/Str
   (s/optional-key :filter) [(s/one TimeFilter "tf") NormalFilter]
   (s/optional-key :split) [Split]
   (s/optional-key :arrived-split) [Split]
   (s/optional-key :measures) [Measure]
   (s/optional-key :pinboard) Pinboard
   (s/optional-key :results) {(s/enum :main :pinboard :filter) (s/cond-pre [Result] {s/Str [Result]})}
   (s/optional-key :last-added-filter) s/Any})

(def AppDB
  {(s/optional-key :page) s/Keyword
   (s/optional-key :loading) {(s/cond-pre s/Keyword [s/Any]) s/Bool}
   (s/optional-key :cubes) {s/Str Cube}
   (s/optional-key :settings) Settings
   (s/optional-key :cube-view) CubeView})
