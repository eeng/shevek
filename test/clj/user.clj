(ns user
  (:require [shevek.acceptance.test-helper :refer :all]))

(defn reset
  "If this function doesn't exists ProtoREPL will try to do his own code reloading, but we are already doing it on boot."
  [])

#_(def page (chrome))
#_(visit page "/")
