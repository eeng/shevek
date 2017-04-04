(ns shevek.db
  (:require [mount.core :refer [defstate]]
            [monger.core :as mg]
            [shevek.config :refer [config]]))

(defstate mongo
  :start (mg/connect-via-uri (config :mongodb-uri))
  :stop (mg/disconnect (mongo :conn)))

(defn db []
  (mongo :db))
