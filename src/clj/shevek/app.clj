(ns shevek.app
  (:require [mount.core :as mount]
            [taoensso.timbre :as log]
            [schema.core :as schema]
            [shevek.env :refer [env]]
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
  (mount/stop)
  (mount/start))

(defn -main [& args]
  (start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop)))
