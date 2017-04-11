(ns shevek.schemas.app-db
  (:require [schema.core :as s]
            [shevek.schemas.cube :refer [Dimension Measure]]
            [shevek.schemas.viewer :refer [Viewer Cube]]))

(s/defschema Settings
  {:lang s/Str})

(s/defschema AppDB
  {(s/optional-key :page) s/Keyword
   (s/optional-key :loading) {(s/cond-pre s/Keyword [s/Any]) s/Bool}
   (s/optional-key :cubes) {s/Str Cube}
   (s/optional-key :settings) (s/maybe Settings)
   (s/optional-key :viewer) Viewer
   (s/optional-key :users) [s/Any]}) ; TODO
