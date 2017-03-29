(ns shevek.db
  (:require [schema.core :as s :include-macros true]))

(def Settings
  {:lang s/Str})

(def AppDB
  {(s/optional-key :page) s/Keyword
   (s/optional-key :loading) {(s/cond-pre s/Keyword [s/Any]) s/Bool}
   (s/optional-key :cubes) {s/Str {s/Keyword s/Any}}
   (s/optional-key :settings) Settings})

(defn schema-checker [interceptor]
  (fn [db event]
    (->> (interceptor db event)
         (s/validate AppDB))))
