(ns shevek.reflow.interceptors.logger
  (:require [shevek.lib.logger :as log :refer [debug?]]
            [clojure.data :as data]))

(defn- clean-for-diff [db]
  (dissoc db :last-events))

(defn logger [interceptor]
  (fn [db event]
    (log/info "Handling event" event "...")
    (if debug?
      (let [start-time (js/Date.)
            new-db (interceptor db event)
            end-time (js/Date.)
            elapsed-time (- end-time start-time)
            [only-before only-after] (data/diff (clean-for-diff db) (clean-for-diff new-db))
            diff-time (- (js/Date.) end-time)
            db-changed? (or (some? only-before) (some? only-after))
            changes (if db-changed?
                      ["with changes from" only-before "to" only-after]
                      ["with no changes."])]
        (apply log/info "Finished event in" elapsed-time "ms" changes)
        new-db)
      (interceptor db event))))
