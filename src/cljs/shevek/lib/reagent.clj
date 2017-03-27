(ns shevek.lib.reagent)

; FIXME en matteo tenia definido el with-react-keys aca pero no parece funcar aca, no encuentra la funcion. Ya me habia pasado lo mismo en reflow.macros. Tb habria que renombrar shevek.lib.{react,reagent}. Tampoco anda el refer-macros como en matteo. Hay que usar si o si require-macros. Revisar.

(defmacro rfor [seq-exprs body-expr]
  `(shevek.lib.react/with-react-keys (doall (for ~seq-exprs ~body-expr))))
