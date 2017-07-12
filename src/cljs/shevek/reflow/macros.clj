(ns shevek.reflow.macros)

(defmacro defevh [event bindings & body]
  `(shevek.reflow.core/register-event-handler ~event (fn ~bindings ~@body)))
