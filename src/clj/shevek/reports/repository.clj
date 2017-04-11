(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.schemas.viewer :as vs]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(s/defschema Filter
  (assoc vs/Filter :dimension s/Str))

(s/defschema Split
  (assoc vs/Split :dimension s/Str))

; TODO falta el pinboard
(s/defschema Report
  {(s/optional-key :_id) ObjectId
   :name s/Str
   (s/optional-key :description) s/Str
   :cube ObjectId
   :measures [s/Str]
   :filter [Filter]
   :split [Split]})

(s/defn save-report [db report :- Report]
  (mc/save-and-return db "reports" report))
