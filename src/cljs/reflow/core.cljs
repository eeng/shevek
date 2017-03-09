(ns reflow.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as r]
            [reflow.handlers :refer [identity-handler]]
            [reflow.utils :refer [log]]))

(def ^:private events (chan))
(def ^:private app-state (r/atom {}))

(defn dispatch [eid & args]
  {:pre [(keyword? eid)]}
  (put! events (into [eid] args)))

(defn- start-coordinator [initial-state handler]
  (go-loop [actual-state @initial-state]
    (when-let [event (<! events)]
      (let [new-state (handler actual-state event)]
        (assert (map? new-state)
                (str "Handler should return the new state as a map. Instead returned: " (pr-str new-state)))
        (reset! initial-state new-state)
        (log "Coordinator state" @initial-state)
        (recur new-state)))))

(defn init [handler]
  (log "Initializing reflow event loop...")
  (start-coordinator app-state handler))
