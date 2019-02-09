(ns shevek.schemas.dashboard
  (:require [schema.core :as s]
            [shevek.schemas.report :refer [Report]]))

(s/defschema Panel
  {:type (s/enum "cube-selector" "report")
   (s/optional-key :report) Report
   (s/optional-key :grid-pos) {:x s/Int :y s/Int :h s/Int :w s/Int}})

(s/defschema Dashboard
  {(s/optional-key :id) s/Str
   :name s/Str
   (s/optional-key :description) s/Str
   (s/optional-key :user-id) s/Str
   (s/optional-key :panels) [Panel]
   (s/optional-key :created-at) s/Any
   (s/optional-key :updated-at) s/Any})
