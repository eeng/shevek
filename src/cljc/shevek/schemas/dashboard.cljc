(ns shevek.schemas.dashboard
  (:require [schema.core :as s]
            [shevek.schemas.report :refer [Report]]))

(s/defschema DashboardReport
  {:report-id s/Str
   (s/optional-key :report) Report}) ; Eager fetched for the UI

(s/defschema Dashboard
  {(s/optional-key :id) s/Str
   :name s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :user-id) s/Str ; When the new dashboard form is displayed we still don't have a user-id
   (s/optional-key :reports) [DashboardReport]
   (s/optional-key :created-at) s/Any
   (s/optional-key :updated-at) s/Any})
