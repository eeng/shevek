(ns pivot.server
  (:require [mount.core :refer [defstate]]))

(defstate web-server
  :start (do (println ">>> starting web server") :started)
  :stop (do (println "<<< stopping web server") :stopped))
