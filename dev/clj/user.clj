(ns user
  (:require [figwheel-sidecar.repl-api :refer [cljs-repl]]))

(defn reset
  "ProtoREPL calls this function when starts and when refreshing namespaces"
  [])

; To start a ClojureScript REPL connect to the figwheel nREPL (port 4002) and then eval this
#_(cljs-repl)
