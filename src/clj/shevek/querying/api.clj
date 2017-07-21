(ns shevek.querying.api
  (:require [shevek.dw :refer [dw]]
            [shevek.querying.aggregation :as agg]
            [shevek.querying.raw :as raw]))

(defn query [_ q]
  (agg/query dw q))

(defn raw-query [_ q]
  (raw/query dw q))
