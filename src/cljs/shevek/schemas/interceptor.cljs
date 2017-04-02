(ns shevek.schemas.interceptor
  (:require [schema.core :as s]
            [shevek.schemas.app-db :refer [AppDB]]
            [shevek.lib.util :refer [debug?]]))

(defn- check-schema [schema db]
  (try
    (s/validate schema db)
    (catch ExceptionInfo e
      (console.log :ERROR (.-message e)))))

(defn checker [interceptor]
  (if debug?
    (fn [db event]
      (->> (interceptor db event)
           (check-schema AppDB)))
    interceptor))

(s/set-fn-validation! debug?)
