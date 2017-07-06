(ns shevek.test-helper
  (:require [mount.core :as mount]
            [monger.db :refer [drop-db]]
            [shevek.app]
            [shevek.db :refer [db init-db]]
            [clojure.test :refer [deftest testing]]
            [cuerdas.core :as str]
            [spyscope.core]))

(defn init-unit-tests []
  (mount/start-without #'shevek.app/nrepl #'shevek.server/web-server))

(defn init-acceptance-tests []
  (mount/start))

(defmacro it [description & body]
  `(testing ~description
     (drop-db db)
     (init-db db)
     ~@body))

(defmacro pending [name & body]
  (let [message (str "\n" name " is pending !!")]
    `(testing ~name (println ~message))))
