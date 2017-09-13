(ns shevek.schemas.dashboard
  (:require [schema.core :as s]))

(s/defschema DashboardReport
  {:report-id s/Any})

(s/defschema Dashboard
  {(s/optional-key :id) s/Str
   :name s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :user-id) s/Str ; When the new dashboard form is displayed we still don't have a user-id
   (s/optional-key :reports) [DashboardReport]
   (s/optional-key :created-at) s/Any
   (s/optional-key :updated-at) s/Any})
