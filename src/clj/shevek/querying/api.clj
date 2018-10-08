(ns shevek.querying.api
  (:require [shevek.engine.protocol :as e]
            [shevek.engine.state :refer [dw]]
            [shevek.db :refer [db]]
            [shevek.schema.repository :refer [find-cube]]
            [shevek.querying.auth :as auth]
            [shevek.schemas.query :refer [Query RawQuery]]
            [schema.core :as s]))

(s/defn query [{:keys [user]}
               {:keys [cube] :as q} :- Query]
  (e/designer-query
   dw
   (auth/filter-query user q)
   (find-cube db cube)))

(s/defn raw-query [{:keys [user]}
                   {:keys [cube] :as q} :- RawQuery]
  (e/raw-query
   dw
   (auth/filter-query user q)
   (find-cube db cube)))
