(ns shevek.reflow.router
  (:require [shevek.lib.logger :as log :refer [debug?]]))

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
  (fn [db [eid & ev-data]]
    (let [[handler interceptors] (@event-handlers eid)]
      (if handler
        (let [new-db (apply invoke-and-handle-return handler db ev-data)
              {:keys [after]} interceptors]
          (if (seq after)
            (reduce (fn [db after-fn] (invoke-and-handle-return after-fn db)) new-db after)
            new-db))
        (do
          (log/error "No handler found for event " eid)
          db)))))
