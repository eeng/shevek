(ns shevek.schemas.report
  (:require [schema.core :as s]
            [shevek.schemas.query :refer [Filters Split Measure]]))

(s/defschema Pinboard
  {:measure s/Str :dimensions [Split]})

(s/defschema Report
  {(s/optional-key :id) s/Str
   (s/optional-key :name) s/Str ; Optional when restored from URL
   (s/optional-key :description) s/Str
   :cube s/Str
   :viztype s/Str
   :measures [Measure]
   :filters Filters
   (s/optional-key :splits) [Split]
   (s/optional-key :pinboard) Pinboard
   (s/optional-key :owner-id) s/Str
   (s/optional-key :shared-by-id) s/Str
   (s/optional-key :sharing-digest) s/Str
   (s/optional-key :dashboards-ids) [s/Str]
   (s/optional-key :created-at) s/Any
   (s/optional-key :updated-at) s/Any})
