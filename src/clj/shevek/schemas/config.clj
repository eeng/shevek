(ns shevek.schemas.config
  (:require [schema.core :as s]))

(s/defschema NotificationsConfig
  {:server {s/Keyword s/Str}
   :errors {:to s/Str}})

(s/defschema MonitoringConfig
  {(s/optional-key :influx) {s/Keyword s/Str}})

(defn- sixteen-chars? [s]
  (= (count s) 16))

(s/defschema Config
  {:env s/Str
   :port s/Int
   :nrepl-port s/Int
   :mongodb-uri s/Str
   :druid-uri s/Str
   :session {:key (s/constrained s/Str sixteen-chars?)
             :timeout s/Int}
   :datasources-discovery-interval s/Int
   :time-boundary-update-interval s/Int
   :log {:level s/Keyword :timestamp s/Bool :to s/Str}
   :notifications NotificationsConfig
   :monitoring MonitoringConfig
   :cubes s/Any}) ; They will be validated on the repository
