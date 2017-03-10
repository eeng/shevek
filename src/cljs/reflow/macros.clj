(ns reflow.macros)

(defmacro defevh [event bindings & body]
  `(reflow.core/register-event-handler ~event (fn ~bindings ~@body)))
