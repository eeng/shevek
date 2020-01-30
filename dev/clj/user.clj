(ns user
  (:require [figwheel-sidecar.repl-api :refer [cljs-repl]]
            [clojure.tools.namespace.repl :as tns]
            [shevek.app :as app]
            [shevek.schema.seed :as seed]
            [shevek.db :refer [db]]))

(defn reset
  "ProtoREPL calls this function when starts and when refreshing namespaces"
  []
  ; So the tns/refresh doesn't try to load tests files
  (tns/set-refresh-dirs "src/clj"))

(comment
  (app/start)
  (app/stop)
  (app/restart)

  ; Restores the database to its initial state
  (do
    (app/restart) ; Reload config
    (seed/db-reset! db))

  (tns/refresh)

  ; To start a ClojureScript REPL connect to the figwheel nREPL (port 4002) and then eval this
  (cljs-repl))
