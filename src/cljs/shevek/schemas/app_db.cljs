(ns shevek.schemas.app-db
  (:require [schema.core :as s]
            [shevek.schemas.cube :refer [Dimension Measure]]
            [shevek.schemas.viewer :as vs])
  (:import goog.date.DateTime))

(s/defschema Settings
  {:lang s/Str})

(s/defschema Cube
  {(s/optional-key :_id) s/Any
   :name s/Str
   (s/optional-key :title) s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :dimensions) [Dimension]
   (s/optional-key :measures) [Measure]
   (s/optional-key :max-time) goog.date.DateTime})

(s/defschema Viewer
  (assoc vs/Viewer :cube Cube))

(s/defschema AppDB
  {(s/optional-key :page) s/Keyword
   (s/optional-key :loading) {(s/cond-pre s/Keyword [s/Any]) s/Bool}
   (s/optional-key :cubes) {s/Str Cube}
   (s/optional-key :settings) (s/maybe Settings)
   (s/optional-key :viewer) Viewer
   (s/optional-key :users) [s/Any]}) ; TODO
