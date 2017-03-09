(ns reflow.handlers
  (:require [reflow.utils :refer [log]]))

(defn identity-handler [state _]
  state)

(defn logging-handler [handler]
  (fn [state event]
    (log "> Firing event" event "with state" state)
    (let [new-state (handler state event)]
      (log "< Ending event" event "with state" new-state)
      new-state)))

(defn recording-handler [handler]
  (fn [state event]
    (log "Recording event" event)
    (-> (handler state event)
        (update :recorded-events (fnil conj []) event))))

(defn router [event-handlers]
  (fn [state [eid _ :as event]]
    (when-let [handler (event-handlers eid)]
      (handler state event))))
