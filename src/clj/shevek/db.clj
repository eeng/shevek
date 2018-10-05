(ns shevek.db
  (:require [mount.core :refer [defstate]]
            [monger.core :as mg]
            [monger.collection :as mc]
            [shevek.config :refer [config env?]]
            [shevek.schema.migrator :refer [migrate!]]
            [taoensso.timbre :as log]))

(defn- connect []
  (log/info "Establishing connection to" (config :mongodb-uri))
  (mg/connect-via-uri (config :mongodb-uri)))

(defstate ^{:on-reload :noop} mongo
  :start (connect)
  :stop (mg/disconnect (mongo :conn)))

(defn init-db [db]
  (mc/ensure-index db "schema-migrations" (array-map :name 1) {:unique true})
  (mc/ensure-index db "cubes" (array-map :name 1) {:unique true})
  (mc/ensure-index db "users" (array-map :username 1) {:unique true})
  (mc/ensure-index db "reports" (array-map :user-id 1 :name 1))
  (mc/ensure-index db "dashboards" (array-map :user-id 1 :name 1))
  (when-not (env? :test) (migrate! db))
  db)

(defstate db :start (init-db (mongo :db)))

(defn clean! [db]
  (mc/purge-many db ["users" "cubes" "reports" "dashboards"]))
