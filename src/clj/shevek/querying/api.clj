(ns shevek.querying.api
  (:require [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]
            [shevek.schema.repository :refer [find-cube]]
            [shevek.querying.aggregation :as agg]
            [shevek.querying.raw :as raw]
            [shevek.querying.auth :as auth]
            [shevek.querying.expansion :refer [expand-query]]
            [shevek.schema.api :refer [max-time]]
            [shevek.schemas.query :refer [Query RawQuery]]
            [schema.core :as s]))

(s/defn query [{:keys [user]} {:keys [cube] :as q} :- Query]
  (->> (find-cube db cube)
       (merge {:max-time (max-time nil cube)}) ; TODO remove
       (expand-query q)
       (auth/filter-query user)
       (agg/query dw)))

(s/defn raw-query [{:keys [user]} {:keys [cube] :as q} :- RawQuery]
  (->> (find-cube db cube)
       (expand-query q)
       (auth/filter-query user)
       (raw/query dw)))
