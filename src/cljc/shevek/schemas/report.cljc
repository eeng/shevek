(ns shevek.schemas.report
  (:require [schema.core :as s]
            [shevek.schemas.query :refer [Filters Split]]))

(s/defschema Pinboard
  {:measure s/Str :dimensions [Split]})

(s/defschema Report
  {(s/optional-key :id) s/Str
   (s/optional-key :name) s/Str ; Optional when restored from URL
   (s/optional-key :description) s/Str
   :cube s/Str
   :viztype s/Str
   :measures [s/Str]
   :filters Filters
   (s/optional-key :splits) [Split]
   :pinboard Pinboard
   :user-id s/Str
   (s/optional-key :dashboards-ids) [s/Str]
   (s/optional-key :created-at) s/Any
   (s/optional-key :updated-at) s/Any})
