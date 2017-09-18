(ns shevek.schema.migrator
  (:require [shevek.lib.collections :refer [includes?]]
            [monger.collection :as mc]
            [taoensso.timbre :as log]
            [shevek.schema.migrations :refer [migrations]]))

(def migrations-collection "schema-migrations")

(defn- filter-already-done [db migrations]
  (let [migrations-done (->> (mc/find-maps db migrations-collection)
                             (map (comp keyword :name)))]
    (remove #(includes? migrations-done (first %)) migrations)))

(defn- insert-migration-version [db migration-name]
  (mc/insert db migrations-collection {:name (name migration-name)}))

(defn migrate!
  ([db] (migrate! db migrations))
  ([db migrations]
   (doseq [[migration-name migration-fn] (filter-already-done db migrations)]
     (log/debug "Executing migration" migration-name)
     (migration-fn db)
     (insert-migration-version db migration-name))
   db))
