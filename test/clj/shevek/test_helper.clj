(ns shevek.test-helper
  (:require [monger.collection :refer [purge-many]]
            [shevek.app :refer [start start-db]]
            [mount.core :as mount]
            [shevek.scheduler :refer [scheduler]]
            [shevek.db :refer [db init-db]]
            [clojure.test :refer [deftest testing]]
            [cuerdas.core :as str]
            [spyscope.core]))

(defn init-unit-tests []
  (start-db))

(defn init-acceptance-tests []
  (mount/start-without #'scheduler))

(defmacro it [description & body]
  `(testing ~description
     (purge-many db ["users" "cubes" "reports" "dashboards"])
     ~@body))

(defmacro pending [name & body]
  (let [message (str "\n" name " is pending !!")]
    `(testing ~name (println ~message))))
