(ns shevek.dashboards.api
  (:require [schema.core :as s]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.dashboards.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.auth :refer [authorize]]))

(s/defn save [{:keys [user-id]} {:keys [id] :as dashboard} :- Dashboard]
  (when id
    (authorize (= user-id (:owner-id (r/find-by-id db id)))))
  (r/save-dashboard db (assoc dashboard :owner-id user-id)))

; TODO authorize here
(defn delete [_ id]
  (r/delete-dashboard db id))

(defn find-all [{:keys [user-id]}]
  (r/find-dashboards db user-id))

(s/defn find-by-id :- Dashboard [{:keys [user-id]} id]
  (r/find-by-id db id))
