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

(defn- do-query [user {:keys [cube] :as q} q-fn]
  (->> (find-cube db cube)
       (merge {:max-time (max-time nil cube)}) ; TODO remove
       (expand-query q)
       (auth/filter-query user)
       (q-fn dw)))

(s/defn query [{:keys [user]} q :- Query]
  (do-query user q agg/query))

(s/defn raw-query [{:keys [user]} q :- RawQuery]
  (do-query user q raw/query))
