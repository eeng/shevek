(ns user)

(defn reset
  "ProtoREPL calls this function when starts and when refreshing namespaces"
  [])

#_(do
    (require '[figwheel-sidecar.repl-api :refer [cljs-repl]])
    (cljs-repl))
