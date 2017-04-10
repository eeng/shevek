(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.querying.schemas :as qs :refer [Viewer]])
  (:import [org.bson.types ObjectId]))

(s/defschema Filter
  (assoc qs/Filter :dimension s/Str))

(s/defschema Split
  (assoc qs/Split :dimension s/Str))

(s/defschema Report
  {:name s/Str
   (s/optional-key :description) s/Str
   :cube ObjectId
   :measures [s/Str]
   :filter [Filter]
   :split [Split]})

(defn- simplify-sort-by [{:keys [dimension measure] :as sort-by}]
  (cond-> sort-by
          dimension (assoc :dimension (:name dimension))
          measure (assoc :measure (:name measure))))

(s/defn viewer->report [{:keys [cube measures filter split]}]
  {:cube (:_id cube)
   :measures (map :name measures)
   :filter (map #(assoc % :dimension (-> % :dimension :name)) filter)
   :split (map #(assoc % :dimension (-> % :dimension :name)
                         :sort-by (simplify-sort-by (:sort-by %)))
               split)})

(defn save-report [report-fields viewer]
  ; TODO finish saving
  (merge report-fields (viewer->report viewer)))

(defn report->viewer [viewer])
