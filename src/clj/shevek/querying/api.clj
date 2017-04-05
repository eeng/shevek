(ns shevek.querying.api
  (:require [shevek.dw2 :refer [dw]]
            [shevek.querying.manager :as m]))

(defn query [q]
  (m/query dw q))
