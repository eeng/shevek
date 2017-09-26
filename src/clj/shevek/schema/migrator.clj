(ns shevek.schema.migrator
  (:require [shevek.lib.collections :refer [includes? stringify-keys]]
            [monger.collection :as mc]
            [taoensso.timbre :as log]
            [shevek.schema.migrations :as ms]))

(def migrations-collection "schema-migrations")

(defn- filter-already-done [db migrations]
  (let [migrations-done (map :name (mc/find-maps db migrations-collection))]
    (remove #(includes? migrations-done (first %)) migrations)))

(defn- insert-migration-version [db migration-name]
  (mc/insert db migrations-collection {:name migration-name}))

(defn migrate!
  ([db] (migrate! db ms/migrations))
  ([db migrations]
   (doseq [[migration-name migration-fn] (->> migrations stringify-keys (filter-already-done db))]
     (log/debug "Executing migration" migration-name)
     (migration-fn db)
     (insert-migration-version db migration-name))
   db))
