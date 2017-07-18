(ns shevek.schemas.app-db
  (:require [schema.core :as s]
            [schema-tools.core :as st]
            [shevek.schemas.cube :refer [Dimension Measure]]
            [shevek.schemas.viewer :refer [Viewer Cube]]
            [shevek.schemas.report :refer [Report]]))

(s/defschema Settings
  {(s/optional-key :lang) s/Str
   (s/optional-key :auto-refresh) s/Int})

(s/defschema Dashboard
  {s/Str Viewer})

(s/defschema CurrentReport
  (st/assoc Report (s/optional-key :user-id) s/Any))

(s/defschema AppDB
  {(s/optional-key :page) s/Keyword
   (s/optional-key :current-user) s/Any
   (s/optional-key :loading) {(s/cond-pre s/Keyword [s/Any]) s/Bool}
   (s/optional-key :cubes) {s/Str Cube}
   (s/optional-key :settings) (s/maybe Settings)
   (s/optional-key :viewer) Viewer
   (s/optional-key :current-report) (s/maybe CurrentReport)
   (s/optional-key :reports) [Report]
   (s/optional-key :users) [s/Any] ; TODO
   (s/optional-key :dashboard) Dashboard})
