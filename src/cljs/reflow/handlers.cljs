(ns reflow.handlers)

(defn identity-handler [state _]
  state)

(defn logging-handler [handler]
  (fn [state event]
    (println "> Firing event" event "with state" state)
    (let [new-state (handler state event)]
      (println "< Ending event" event "with state" new-state)
      new-state)))

(defn recording-handler [handler]
  (fn [state event]
    (update state :events (fnil conj []) event)))
