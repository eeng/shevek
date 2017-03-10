(ns reflow.macros)

(defmacro defevh [event bindings & body]
  `(reflow.interceptors/register-event-handler ~event (fn ~bindings ~@body)))
