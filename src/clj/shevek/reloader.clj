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
  (loop [tracker (tracker/ns-tracker ["src"])]
    (reload-namespaces (tracker))
    (Thread/sleep 1000)
    (recur tracker)))

(defn start-reloader []
  (log/info "Starting reloader")
  (doto (Thread. auto-reloader)
    (.setDaemon true)
    (.start)))

(defstate ^{:on-reload :noop} reloader
  :start (start-reloader)
  :stop (.stop reloader))

; To manually restart the reloader in case of changes in this file, as the state has to have a on-reload
#_(mount.core/stop #'reloader)
#_(mount.core/start #'reloader)
