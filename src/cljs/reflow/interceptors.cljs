(ns reflow.interceptors
  (:require [reflow.utils :refer [log]]
            [clojure.data :as data]))

(defn logger [interceptor]
  (fn [db event]
    (log "Handling event" event)
    (let [new-db (interceptor db event)
          [only-before only-after] (data/diff db new-db)
          db-changed? (or (some? only-before) (some? only-after))]
      (if db-changed?
        (log "Finished event" event "with changes: only before" only-before "only after" only-after)
        (log "Finished event" event "with no changes."))
      new-db)))

(defn recorder [interceptor]
  (fn [db event]
    (log "  Recording event" event)
    (-> (interceptor db event)
        (update :recorded-events (fnil conj []) event))))

(def ^:private event-handlers (atom {}))

(defn register-event-handler [eid fn]
  (swap! event-handlers assoc eid fn))

(defn router []
  (fn [db [eid _ :as event]]
    (let [interceptor (@event-handlers eid)]
      (assert interceptor (str "No handler found for event " eid))
      (interceptor db event))))
