(ns user
  (:require [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :refer [chrome go query-all]]))

(defn reset
  "If this function doesn't exists ProtoREPL will try to do his own code reloading, but we are already doing it on boot."
  [])

; Procedure for acceptance tests creation:
; 1. Run on one terminal: boot watch test-acceptance
; 2. In another: boot repl -c -p 4101 (or connect with ProtoREPL)
; 3. Create the page and navigate to the dev server so the queries works against a real Druid
#_(def page (chrome))
#_(go page "http://localhost:4000")
