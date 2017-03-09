(ns user
  (:require [mount.core :as mount]
            [clojure.tools.namespace.repl :as tn]
            [pivot.app]))

(defn start
  "Starts only the necessary services (not the nrepl)."
  []
  (mount/start-without #'pivot.app/nrepl))

(defn stop []
  (mount/stop))

(defn reset
  "Stops all states defined by defstate, reloads modified source files, and restarts the states."
  []
  (stop)
  (tn/refresh :after 'user/start))

; Para transformar el REPL en bREPL:
; (in-ns 'boot.user) (start-repl)
