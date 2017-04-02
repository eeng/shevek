(ns shevek.test-helper
  (:require [mount.core :as mount]
            [shevek.app]))

(defn init []
  (mount/start-without #'shevek.app/nrepl #'shevek.server/web-server))
