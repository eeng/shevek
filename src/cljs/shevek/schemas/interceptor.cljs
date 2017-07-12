(ns shevek.schemas.interceptor
  (:require [schema.core :as s]
            [shevek.schemas.app-db :refer [AppDB]]
            [shevek.lib.logger :refer [debug?]]))

(defn checker [interceptor]
  (if debug?
    (fn [db event]
      (->> (interceptor db event)
           (s/validate AppDB)))
    interceptor))

(s/set-fn-validation! debug?)
