(ns reflow.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reflow.db :refer [app-db]]
            [reflow.utils :refer [log]]
            [reflow.interceptors :as i]))

(defonce events (chan))
(defonce coordinator (atom nil))

(defn dispatch [eid & args]
  {:pre [(keyword? eid)]}
  (put! events (into [eid] args)))

(def register-event-handler i/register-event-handler)

(defn- start-coordinator [app-db handler]
  (go-loop []
    (let [event (<! events)]
      (when (and event (not= event [:shutdown]))
        (let [new-db (swap! app-db handler event)]
          (assert (map? new-db)
                  (str "Handler for event " event " should've returned the new db as a map. Instead returned: " (pr-str new-db)))
          (recur))))))

(defn stop-coordinator []
  (dispatch :shutdown))

(defn init [handler]
  (when @coordinator
    (dispatch :shutdown))
  (log "Starting reflow coordinator")
  (reset! coordinator (start-coordinator app-db handler)))
