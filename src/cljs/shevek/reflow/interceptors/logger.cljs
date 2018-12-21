(ns shevek.reflow.interceptors.logger
  (:require [shevek.lib.logger :as log :refer [debug?]]
            [clojure.data :as data]))

(defn logger [interceptor & {:keys [keys-excluded-from-diff]
                             :or {keys-excluded-from-diff [:last-events]}}]
  (fn [db event]
    (log/info "Handling event" event "...")
    (if debug?
      (let [new-db (interceptor db event)
            [only-before only-after] (data/diff
                                      (apply dissoc db keys-excluded-from-diff)
                                      (apply dissoc new-db keys-excluded-from-diff))
            db-changed? (or (some? only-before) (some? only-after))]
        (if db-changed?
          (log/info "Finished event with changes: before" only-before "after" only-after)
          (log/info "Finished event with no changes."))
        new-db)
      (interceptor db event))))
