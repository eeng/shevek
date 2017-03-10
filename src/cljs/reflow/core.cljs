(ns reflow.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as r]
            [reflow.utils :refer [log]]))

(def ^:private events (chan))
(def ^:private app-db (r/atom {}))

(defn dispatch [eid & args]
  {:pre [(keyword? eid)]}
  (put! events (into [eid] args)))

(defn- start-coordinator [app-db handler]
  (go-loop [actual-db @app-db]
    (when-let [event (<! events)]
      (let [new-db (handler actual-db event)]
        (assert (map? new-db)
                (str "Handler should return the new db as a map. Instead returned: " (pr-str new-db)))
        (reset! app-db new-db)
        (recur new-db)))))

(defn init [handler]
  (log "Initializing reflow event loop")
  (start-coordinator app-db handler))

(defn debug-db []
  [:pre (with-out-str (cljs.pprint/pprint @app-db))])
