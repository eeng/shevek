(ns shevek.reloader
  (:require [ns-tracker.core :as tracker]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as log]
            [shevek.env :as env]))

(defn- reload-namespaces [changed-ns]
  (try
    (when (seq changed-ns)
      (println "Reloading" changed-ns)
      (doseq [ns-sym changed-ns]
        (require ns-sym :reload)))
    (catch Throwable e
      (log/error e))))

(defn- code-reloader []
  (let [tracker (tracker/ns-tracker ["src"])]
    (Thread/sleep 1000)
    (reload-namespaces (tracker))))

(defn- start-infinite [f]
  (let [running (atom true)]
    (when (env/development?)
      (log/info "Starting code reloader")
      (future
       (loop []
         (when @running
           (f)
           (recur)))))
    running))

(defn- stop-infinite [running]
  (reset! running false))

(defstate reloader
  :start (start-infinite code-reloader)
  :stop (stop-infinite reloader))

#_(mount.core/start #'shevek.config/cfg #'reloader)
#_(mount.core/stop #'shevek.config/cfg #'reloader)
