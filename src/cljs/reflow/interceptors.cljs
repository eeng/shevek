(ns reflow.interceptors
  (:require [reflow.utils :refer [log]]
            [clojure.data :as data]))

(defn logger [interceptor]
  (if ^boolean goog.DEBUG
    (fn [db event]
      (log "Handling event" event "...")
      (let [new-db (interceptor db event)
            [only-before only-after] (data/diff db new-db)
            db-changed? (or (some? only-before) (some? only-after))]
        (if db-changed?
          (log "Finished event with changes: before" only-before "after" only-after)
          (log "Finished event with no changes."))
        new-db))
    interceptor))

(defn recorder [interceptor]
  (fn [db event]
    (-> (interceptor db event)
        (update :recorded-events (fnil conj []) event))))

(def ^:private event-handlers (atom {}))

(defn register-event-handler [eid handler]
  (swap! event-handlers assoc eid handler))

(defn router []
  (fn [db [eid & ev-data :as event]]
    (let [interceptor (@event-handlers eid)]
      (assert interceptor (str "No handler found for event " eid))
      (apply interceptor db ev-data))))
