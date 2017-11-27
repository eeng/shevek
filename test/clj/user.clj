(ns user
  (:require [shevek.acceptance.test-helper :refer :all]
            [etaoin.api :as e]))

(defn reset
  "If this function doesn't exists ProtoREPL will try to do his own code reloading, but we are already doing it on boot."
  [])

; Workflow for acceptance tests creation:
; 1. Run on one terminal: boot manual-test-acceptance (should open a browser)
; 2. Connect to the repl with ProtoREPL
; 3. Build the test step by step evaluating the forms on the repl (works with "testing" blocks too). Ej:
#_(login)
