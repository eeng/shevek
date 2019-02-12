(ns shevek.dashboards.api
  (:require [schema.core :as s]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.dashboards.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.auth :refer [authorize]]))

(s/defn save [{:keys [user-id]} dashboard :- Dashboard]
  (r/save-dashboard db (assoc dashboard :owner-id user-id)))

(defn delete [_ id]
  (r/delete-dashboard db id))

(defn find-all [{:keys [user-id]}]
  (r/find-dashboards db user-id))

(s/defn find-by-id :- Dashboard [{:keys [user-id]} id]
  (let [dashboard (r/find-by-id db id)]
    (authorize (= (:owner-id dashboard) user-id))
    dashboard))

#_(find-by-id {:user-id "5c4a064444d29c076e5b1219"} "5c4cafbb44d29c055644add6")
#_(find-all {:user-id "5c4a064444d29c076e5b1219"})
