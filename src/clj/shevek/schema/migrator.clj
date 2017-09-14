(ns shevek.schema.migrator
  (:require [shevek.lib.collections :refer [includes?]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.find :refer [find-namespaces-in-dir]]
            [monger.collection :as mc]
            [taoensso.timbre :as log]))

(def migrations-collection "schema-migrations")

(defn- version [ns-sym]
  (->> ns-sym name (re-find #"\.(\d+)\-.+") last))

(defn- filter-already-done [db migrations]
  (let [db-versions (map :version (mc/find-maps db migrations-collection))]
    (->> migrations
         (filter version)
         (remove #(includes? db-versions (version %))))))

(defn- insert-migration-version [db migration-ns]
  (mc/insert db migrations-collection {:version (version migration-ns)}))

(defn migrate!
  ([db] (migrate! db "shevek/migrations"))
  ([db migrations-dir]
   (let [migrations (->> migrations-dir
                         io/resource io/file
                         find-namespaces-in-dir
                         (filter-already-done db))]
     (doseq [migration migrations]
       (log/debug "Executing migration" migration)
       ((ns-resolve migration 'up) db)
       (insert-migration-version db migration))
    db)))
