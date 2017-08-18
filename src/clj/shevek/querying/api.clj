(ns shevek.querying.api
  (:require [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]
            [shevek.schema.repository :refer [find-cube]]
            [shevek.querying.aggregation :as agg]
            [shevek.querying.raw :as raw]))

(defn query [_ {:keys [cube] :as q}]
  (agg/query dw (find-cube db cube) q))

(defn raw-query [_ {:keys [cube] :as q}]
  (raw/query dw (find-cube db cube) q))
