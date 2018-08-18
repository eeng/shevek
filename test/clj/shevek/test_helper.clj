(ns shevek.test-helper
  (:require [monger.collection :refer [purge-many]]
            [shevek.app :refer [start]]
            [mount.core :as mount]
            [shevek.db :refer [db init-db]]
            [clojure.test :refer [deftest testing]]
            [cuerdas.core :as str]
            [shevek.acceptance.test-helper]))

(defn wrap-unit-tests [f]
  (System/setProperty "conf" "test/resources/test-config.edn")
  (mount/start-without #'shevek.nrepl/nrepl
                       #'shevek.scheduler/scheduler
                       #'shevek.reloader/reloader
                       #'shevek.web.server/web-server
                       #'shevek.acceptance.test-helper/page)
  (f)
  (mount/stop))

(defmacro it [description & body]
  `(testing ~description
     (purge-many db ["users" "cubes" "reports" "dashboards"])
     ~@body))

(defmacro pending [name & body]
  (let [message (str "\n" name " is pending !!")]
    `(testing ~name (println ~message))))
