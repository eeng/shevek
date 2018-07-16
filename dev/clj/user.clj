(ns user
  (:require [clojure.tools.namespace.repl :as tnr]))

(defn reset
  "ProtoREPL calls this function when starts and when refreshing namespaces"
  [])
  ; Previously we used to use this to manually trigger code refreshing with ProtoREPL. Now with the reloader it's no longer needed
  ; (tnr/refresh))

#_(do
    (require '[figwheel-sidecar.repl-api :refer [cljs-repl]])
    (cljs-repl))
