(ns shevek.schemas.interceptor
  (:require [schema.core :as s]
            [shevek.schemas.app-db :refer [AppDB]]
            [shevek.lib.logger :refer [debug?]]))

(defn checker [interceptor]
  (if debug?
    (fn [db [eid :as event]]
      (cond->> (interceptor db event)
               ; Don't validate on this events as it would provoke an infinite loop
               (not (some #{:errors/unexpected-error :errors/show-page} [eid])) (s/validate AppDB)))
    interceptor))

(s/set-fn-validation! debug?)
