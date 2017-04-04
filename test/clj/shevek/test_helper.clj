(ns shevek.test-helper
  (:require [mount.core :as mount]
            [monger.db :refer [drop-db]]
            [shevek.app]
            [shevek.db :refer [db]]))

(defn init []
  (mount/start-without #'shevek.app/nrepl #'shevek.server/web-server))

(defn with-clean-db [f]
  (drop-db (db))
  (f))
