(ns shevek.querying.api
  (:require [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]
            [shevek.schema.repository :refer [find-cube]]
            [shevek.querying.aggregation :as agg]
            [shevek.querying.raw :as raw]))

(defn- expand-with-schema [{:keys [cube] :as q}]
  (let [{:keys [default-time-zone]} (find-cube db cube)]
    (cond-> q
            default-time-zone (assoc :time-zone default-time-zone))))

(defn query [_ q]
  (agg/query dw (expand-with-schema q)))

(defn raw-query [_ q]
  (raw/query dw (expand-with-schema q)))
