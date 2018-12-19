(ns shevek.schemas.interceptor
  (:require [schema.core :as s]
            [shevek.schemas.app-db :refer [AppDB]]
            [shevek.lib.logger :refer [debug?]]))

(defn checker [interceptor]
  (if debug?
    (fn [db [eid :as event]]
      (cond->> (interceptor db event)
               (not= eid :reflow/event-handler-error) (s/validate AppDB)))
    interceptor))

(s/set-fn-validation! debug?)
