(ns shevek.querying.api
  (:require [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]
            [shevek.querying.aggregation :as agg]
            [shevek.querying.raw :as raw]))

(defn query [_ q]
  (agg/query dw db q))

(defn raw-query [_ q]
  (raw/query dw q))
