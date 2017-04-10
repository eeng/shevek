(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.querying.schemas :refer [Viewer]])
  (:import [org.bson.types ObjectId]))

(s/defschema Report
  {:name s/Str
   (s/optional-key :description) s/Str
   :cube-id ObjectId
   :measures [s/Str]})

(s/defn viewer->report [{:keys [cube measures]} :- Viewer]
  {:cube-id (:_id cube)
   :measures (map :name measures)})

(defn save-report [report-fields viewer]
  ; TODO finish saving
  (merge report-fields (viewer->report viewer)))

(defn report->viewer [viewer])
