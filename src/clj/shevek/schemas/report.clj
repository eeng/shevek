(ns shevek.schemas.report
  (:require [schema.core :as s]
            [shevek.schemas.viewer :as vs]
            [shevek.schemas.cube :as cs])
  (:import [org.bson.types ObjectId]))

(def keys-to-remove-from-viewer [:type :title :description])

(s/defschema Filter
  (apply dissoc vs/Filter keys-to-remove-from-viewer))

(s/defschema Split
  (apply dissoc vs/Split keys-to-remove-from-viewer))

; TODO falta el pinboard
(s/defschema Report
  {(s/optional-key :_id) ObjectId
   :name s/Str
   (s/optional-key :description) s/Str
   :cube ObjectId
   :measures [s/Str]
   :filter [Filter]
   :split [Split]})
