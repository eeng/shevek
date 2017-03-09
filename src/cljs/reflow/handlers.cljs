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
    (update state :events (fnil conj []) event)))
