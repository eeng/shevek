(ns shevek.reports.repository
  (:require [schema.core :as s]
            [shevek.schemas.viewer :as vs]
            [shevek.schemas.cube :as cs]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(s/defschema Filter
  (dissoc vs/Filter :type :title :description))

(s/defschema Split
  (dissoc vs/Split :type :title :description))

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
