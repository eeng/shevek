(ns user)

(defn reset
  "If this function doesn't exists ProtoREPL will try to do his own code reloading, but we are already doing it on boot."
  [])

; Para transformar el REPL en bREPL:
; (in-ns 'boot.user) (start-repl)

; Debugging acceptance test
#_(do
    (require '[etaoin.api :refer :all])
    (def page (chrome))
    (go page "http://localhost:3200"))
