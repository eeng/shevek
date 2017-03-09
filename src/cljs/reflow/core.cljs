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

(defn- start-coordinator [initial-db handler]
  (go-loop [actual-db @initial-db]
    (when-let [event (<! events)]
      (let [new-db (handler actual-db event)]
        (assert (map? new-db)
                (str "Handler should return the new db as a map. Instead returned: " (pr-str new-db)))
        (reset! initial-db new-db)
        (recur new-db)))))

(defn init [handler]
  (log "Initializing reflow event loop")
  (start-coordinator app-db handler))

; To visualize the db in the UI add:
; [:pre (with-out-str (cljs.pprint/pprint @reflow.core/app-db))]
