(ns shevek.reflow.interceptors.logger
  (:require [shevek.lib.logger :as log :refer [debug?]]
            [clojure.data :as data]))

(defn logger [interceptor & {:keys [keys-excluded-from-diff]
                             :or {keys-excluded-from-diff [:last-events]}}]
  (fn [db event]
    (log/info "Handling event" event "...")
    (if debug?
      (let [start-time (js/Date.)
            new-db (interceptor db event)
            end-time (js/Date.)
            elapsed-time (- end-time start-time)
            [only-before only-after] (data/diff
                                      (apply dissoc db keys-excluded-from-diff)
                                      (apply dissoc new-db keys-excluded-from-diff))
            diff-time (- (js/Date.) end-time)
            db-changed? (or (some? only-before) (some? only-after))
            changes (if db-changed?
                      ["with changes: before" only-before "after" only-after]
                      ["with no changes."])]
        (apply log/info "Finished event in" elapsed-time "ms" (conj changes (str "(diff: " diff-time " ms)")))
        new-db)
      (interceptor db event))))
