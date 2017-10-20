(ns shevek.querying.api
  (:require [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]
            [shevek.schema.repository :refer [find-cube]]
            [shevek.querying.aggregation :as agg]
            [shevek.querying.raw :as raw]
            [shevek.querying.auth :as auth]
            [shevek.querying.expansion :refer [expand-query]]))

(defn- expand-with-schema [{:keys [cube] :as q}]
  (let [{:keys [default-time-zone]} (find-cube db cube)]
    (cond-> q
            default-time-zone (assoc :time-zone default-time-zone))))

(defn query [{:keys [user]} q]
  (->> (expand-with-schema q)
       (auth/filter-query user)
       (agg/query dw)))

(defn raw-query [{:keys [user]} q]
  (->> (expand-with-schema q)
       (auth/filter-query user)
       (raw/query dw)))
