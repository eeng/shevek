(ns shevek.test-helper
  (:require [monger.collection :refer [purge-many]]
            [shevek.app :refer [start start-db]]
            [mount.core :as mount]
            [shevek.schema.refresher :refer [refresher]]
            [shevek.db :refer [db init-db]]
            [clojure.test :refer [deftest testing]]
            [cuerdas.core :as str]
            [spyscope.core]))

(defn init-unit-tests []
  (start-db))

(defn init-acceptance-tests []
  (mount/start-without #'refresher))

(defmacro it [description & body]
  `(testing ~description
     (purge-many db ["schema-migrations" "users" "cubes" "reports" "dashboards"])
     (init-db db)
     ~@body))

(defmacro pending [name & body]
  (let [message (str "\n" name " is pending !!")]
    `(testing ~name (println ~message))))
