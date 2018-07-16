(ns shevek.test-helper
  (:require [monger.collection :refer [purge-many]]
            [shevek.app :refer [start]]
            [mount.core :as mount]
            [shevek.db :refer [db init-db]]
            [shevek.acceptance.test-helper]
            [clojure.test :refer [deftest testing]]
            [cuerdas.core :as str]))

(defn init-unit-tests []
  (mount/start-without #'shevek.nrepl/nrepl
                       #'shevek.web.server/web-server
                       #'shevek.scheduler/scheduler
                       #'shevek.acceptance.test-helper/page))

(defn init-acceptance-tests []
  (mount/start-without #'shevek.nrepl/nrepl
                       #'shevek.scheduler/scheduler))

(def stop-tests mount/stop)

(defmacro it [description & body]
  `(testing ~description
     (purge-many db ["users" "cubes" "reports" "dashboards"])
     ~@body))

(defmacro pending [name & body]
  (let [message (str "\n" name " is pending !!")]
    `(testing ~name (println ~message))))
