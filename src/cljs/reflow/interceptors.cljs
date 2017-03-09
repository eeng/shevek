(ns reflow.interceptors
  (:require [reflow.utils :refer [log]]))

(defn logger [interceptor]
  (fn [db event]
    (log "> Firing event" event "with db" db)
    (let [new-db (interceptor db event)]
      (log "< Ending event" event "with db" new-db)
      new-db)))

(defn recorder [interceptor]
  (fn [db event]
    (log "  Recording event" event)
    (-> (interceptor db event)
        (update :recorded-events (fnil conj []) event))))

(defn router [event-handlers]
  (fn [db [eid _ :as event]]
    (when-let [interceptor (event-handlers eid)]
      (interceptor db event))))
