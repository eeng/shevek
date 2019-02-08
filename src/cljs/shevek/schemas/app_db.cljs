(ns shevek.schemas.app-db
  (:require [schema.core :as s]
            [schema-tools.core :as st]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.schemas.query :refer [Results]]
            [shevek.schemas.designer :refer [Designer]]
            [shevek.schemas.report :refer [Report]]
            [shevek.schemas.dashboard :refer [Dashboard Panel]]))

(s/defschema Settings
  {(s/optional-key :lang) s/Str
   (s/optional-key :auto-refresh) s/Int
   (s/optional-key :abbreviations) s/Str})

(s/defschema Preferences
  {(s/optional-key :lang) s/Str
   (s/optional-key :auto-refresh) s/Int
   (s/optional-key :abbreviations) s/Str
   (s/optional-key :sidebar-visible) s/Bool})

(s/defschema CurrentDashboard
  (st/assoc Dashboard
            :panels [(st/assoc Panel :id s/Int)]
            (s/optional-key :reports-results) {s/Int Results}))

(s/defschema SelectedPanel
  {:id s/Int :fullscreen s/Bool :edit s/Bool})

(s/defschema Error
  {(s/optional-key :title) s/Str
   :message s/Str})

(s/defschema AppDB
  {(s/optional-key :page) s/Keyword
   (s/optional-key :current-user) s/Any
   (s/optional-key :initialized) s/Bool ; Used to prevent the login form from appearing briefly when a user is logged in and the app is initializing
   (s/optional-key :loading) {(s/cond-pre s/Keyword [s/Any]) s/Bool}
   (s/optional-key :error) Error
   (s/optional-key :cubes) {s/Str Cube}
   (s/optional-key :settings) (s/maybe Settings) ; TODO DASHBOARD vuela
   (s/optional-key :preferences) Preferences
   (s/optional-key :designer) Designer
   (s/optional-key :reports) [Report]
   (s/optional-key :users) [s/Any]
   (s/optional-key :dashboards) [Dashboard]
   (s/optional-key :current-dashboard) CurrentDashboard ; The new/selected dashboard
   (s/optional-key :selected-panel) (s/maybe SelectedPanel)
   (s/optional-key :last-events) [s/Any]})
