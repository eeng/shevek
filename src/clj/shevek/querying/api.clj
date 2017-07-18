(ns shevek.querying.api
  (:require [shevek.dw :refer [dw]]
            [shevek.querying.manager :as m]
            [shevek.querying.raw :as raw]))

(defn query [_ q]
  (m/query dw q))

(defn raw-query [_ q]
  (raw/query dw q))
