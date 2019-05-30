(ns shevek.reloader
  (:require [ns-tracker.core :as tracker]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as log]))

(defn- reload-namespaces [changed-ns]
  (try
    (when (seq changed-ns)
      (println "Reloading" changed-ns)
      (doseq [ns-sym changed-ns]
        (require ns-sym :reload)))
    (catch Throwable e
      (log/error e))))

(defn- auto-reloader []
  (let [tracker (tracker/ns-tracker ["src"])]
    (Thread/sleep 1000)
    (reload-namespaces (tracker))))

(defn- start-infinite [f]
  (let [running (atom true)]
    (future
     (loop []
       (when @running
         (f)
         (recur))))
    running))

(defn- stop-infinite [running]
  (reset! running false))

(defstate reloader
  :start (start-infinite auto-reloader)
  :stop (stop-infinite reloader))

#_(mount.core/start #'reloader)
#_(mount.core/stop #'reloader)
