(ns shevek.reflow.core)

(defmacro defevhi [event bindings interceptors & body]
  {:pre [(keyword? event) (vector? bindings) (map? interceptors)]}
  `(shevek.reflow.core/register-event-handler ~event (fn ~bindings ~@body) ~interceptors))

(defmacro defevh [event bindings & body]
  `(defevhi ~event ~bindings {} ~@body))
