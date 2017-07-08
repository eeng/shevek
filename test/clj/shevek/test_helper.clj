(ns shevek.test-helper
  (:require [monger.db :refer [drop-db]]
            [shevek.app :refer [start start-db]]
            [shevek.db :refer [db init-db]]
            [clojure.test :refer [deftest testing]]
            [cuerdas.core :as str]
            [spyscope.core]))

(defn init-unit-tests []
  (start-db))

(defn init-acceptance-tests []
  (start))

(defmacro it [description & body]
  `(testing ~description
     (drop-db db)
     (init-db db)
     ~@body))

(defmacro pending [name & body]
  (let [message (str "\n" name " is pending !!")]
    `(testing ~name (println ~message))))
