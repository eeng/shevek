(ns shevek.app
  (:require [mount.core :as mount]
            [shevek.init]
            [shevek.web.server :refer [web-server]]
            [shevek.nrepl :refer [nrepl]]
            [shevek.db :refer [db]]
            [shevek.dw]
            [shevek.scheduler :refer [scheduler]]
            [shevek.reloader :refer [reloader]]
            [shevek.schema.seed :refer [seed!]])
  (:gen-class))

(defn start-for-dev []
  (mount/start))

(defn- start-for-seed []
  (mount/start-without #'nrepl #'web-server #'scheduler #'reloader))

(defn start []
  (mount/start-without #'reloader))

(defn- stop []
  (mount/stop))

(defn seed []
  (start-for-seed)
  (seed! db))

(defn -main [& args]
  (start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop)))
