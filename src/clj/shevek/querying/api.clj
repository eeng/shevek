(ns shevek.querying.api
  (:require [shevek.dw :refer [dw]]
            [shevek.db :refer [db]]
            [shevek.schema.repository :refer [find-cube]]
            [shevek.querying.aggregation :as agg]
            [shevek.querying.raw :as raw]
            [shevek.querying.auth :as auth]
            [shevek.querying.expansion :refer [expand-query]]
            [shevek.schemas.query :refer [Query RawQuery]]
            [shevek.schema.metadata :refer [time-boundary]]
            [com.rpl.specter :refer [selected-any? must ALL]]
            [cuerdas.core :as str]
            [schema.core :as s]))

(defn- latest-period-query? [q]
  (selected-any? [:filters ALL :period #(str/starts-with? % "latest")] q))

; TODO throtlelear time-boundary?
(defn- update-max-time [{:keys [name] :as cube} q]
  (cond-> cube
          (latest-period-query? q) (assoc :max-time (:max-time (time-boundary dw name)))))

(defn- do-query [user {:keys [cube] :as q} q-fn]
  (let [cube (-> (find-cube db cube)
                 (update-max-time q))]
    (as-> (auth/filter-query user q) q
          (expand-query q cube)
          (q-fn dw q))))

(s/defn query [{:keys [user]} q :- Query]
  (do-query user q agg/query))

(s/defn raw-query [{:keys [user]} q :- RawQuery]
  (do-query user q raw/query))
