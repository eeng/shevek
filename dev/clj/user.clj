(ns user
  (:require [figwheel-sidecar.repl-api :refer [cljs-repl]]))

(defn reset
  "ProtoREPL calls this function when starts and when refreshing namespaces"
  [])

#_(clojure.tools.namespace.repl/refresh)

; To start a ClojureScript REPL connect to the figwheel nREPL (port 4002) and then eval this
#_(cljs-repl)

; Use this to reread the config file
#_(shevek.app/reload-config)

#_(shevek.schema.seed/db-reset! shevek.db/db)
