(ns shevek.app
  (:require [mount.core :as mount]
            [shevek.init]
            [shevek.web.server :refer [web-server]]
            [shevek.nrepl :refer [nrepl]]
            [shevek.db :refer [db]]
            [shevek.dw]
            [shevek.scheduler :refer [scheduler]]
            [shevek.schema.seed :refer [seed!]])
  (:gen-class))

(defn start-for-dev []
  (mount/start-without #'nrepl))

(defn start-db []
  (mount/start-without #'nrepl #'web-server #'scheduler))

(defn start []
  (mount/start))

(defn seed []
  (start-db)
  (seed! db))

(defn -main [& args]
  (start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. mount/stop)))
