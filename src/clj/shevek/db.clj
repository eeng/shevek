(ns shevek.db
  (:require [mount.core :refer [defstate]]
            [monger.core :as mg]
            [monger.collection :as mc]
            [shevek.config :refer [config]]))

(defstate mongo
  :start (mg/connect-via-uri (config :mongodb-uri))
  :stop (mg/disconnect (mongo :conn)))

(defn init-db [db]
  (mc/ensure-index db "cubes" (array-map :name 1) {:unique true})
  (mc/ensure-index db "users" (array-map :username 1) {:unique true})
  db)

(defstate db :start (init-db (mongo :db)))
