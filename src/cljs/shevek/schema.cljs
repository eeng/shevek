(ns shevek.schema
  (:require [schema.core :as s :include-macros true]
            [schema-tools.core :as st]
            [shevek.lib.util :refer [debug?]]))

(def Settings
  {:lang s/Str})

(def Dimension
  {:name s/Str
   (s/optional-key :title) s/Str ; Opcional por las Query. Ej en la measure del event :filter-values-requested
   :type s/Str})

(def Measure Dimension)

(def DimensionConfig
  (assoc Dimension
         :cardinality (s/maybe s/Int)))

(def Cube
  {:name s/Str
   :title s/Str
   (s/optional-key :dimensions) [DimensionConfig]
   (s/optional-key :measures) [Measure]
   (s/optional-key :max-time) goog.date.DateTime})

(def Split
  (assoc Dimension
         :limit (s/cond-pre s/Int s/Str)
         (s/optional-key :sort-by) (assoc Measure :descending s/Bool)
         (s/optional-key :granularity) s/Str))

(def TimeFilter
  (assoc Dimension
         :name (s/eq "__time")
         :selected-period s/Keyword))

(def NormalFilter
  (assoc Dimension
         :operator s/Str
         (s/optional-key :value) (s/cond-pre s/Str #{(s/maybe s/Str)}))) ; Para el CubeView es siempre un set, pero para la Query que trae los valores que matchean es un str

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

(def Query
  (assoc (st/select-keys CubeView [:cube :filter :split :measures])
         (s/optional-key :totals) s/Bool))

(def AppDB
  {(s/optional-key :page) s/Keyword
   (s/optional-key :loading) {(s/cond-pre s/Keyword [s/Any]) s/Bool}
   (s/optional-key :cubes) {s/Str Cube}
   (s/optional-key :settings) Settings
   (s/optional-key :cube-view) CubeView})

(defn checker [interceptor]
  (fn [db event]
    (->> (interceptor db event)
         (s/validate AppDB))))

(s/set-fn-validation! debug?)
