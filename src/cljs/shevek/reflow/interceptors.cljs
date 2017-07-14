(ns shevek.reflow.interceptors
  (:require [shevek.lib.logger :as log :refer [debug?]]
            [clojure.data :as data]))

(defn logger [interceptor]
  (fn [db event]
    (log/info "Handling event" event "...")
    (if debug?
      (let [new-db (interceptor db event)
            [only-before only-after] (data/diff db new-db)
            db-changed? (or (some? only-before) (some? only-after))]
        (if db-changed?
          (log/info "Finished event with changes: before" only-before "after" only-after)
          (log/info "Finished event with no changes."))
        new-db)
      (interceptor db event))))

(defn recorder [interceptor]
  (fn [db event]
    (-> (interceptor db event)
        (update :recorded-events (fnil conj []) event))))

(def ^:private event-handlers (atom {}))

(defn register-event-handler
  ([eid handler] (register-event-handler eid handler []))
  ([eid handler interceptors] (swap! event-handlers assoc eid [handler interceptors])))

(defn invoke-and-handle-return
  [f db & args]
  "Allow to indicate in handler that a non map return value means no db change was produced"
  (let [new-db (apply f db args)]
    (if (map? new-db) new-db db)))

(defn router []
  (fn [db [eid & ev-data :as event]]
    (let [[handler interceptors] (@event-handlers eid)]
      (assert handler (str "No handler found for event " eid))
      (let [new-db (apply invoke-and-handle-return handler db ev-data)
            {:keys [after]} interceptors]
        (if (seq after)
          (reduce (fn [db after-fn] (invoke-and-handle-return after-fn db)) new-db after)
          new-db)))))

(defn dev-only [next-interceptor interceptor]
  (if ^boolean goog.DEBUG
    (interceptor next-interceptor)
    next-interceptor))
