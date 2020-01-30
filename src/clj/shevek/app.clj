(ns shevek.app
  (:require [mount.core :as mount]
            [schema.core :as schema]
            [shevek.lib.logging]
            [shevek.web.server]
            [shevek.nrepl :refer [nrepl]]
            [shevek.db]
            [shevek.engine.state]
            [shevek.scheduler]
            [shevek.reloader])
  (:gen-class))

(defn start []
  (schema/set-fn-validation! true)
  (mount/start))

(defn stop []
  (mount/stop-except #'nrepl))

(defn restart []
  (stop)
  (start))

(defn -main [& _args]
  (start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop)))
