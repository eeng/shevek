(ns shevek.dashboards.api
  (:require [schema.core :as s]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.dashboards.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.auth :refer [authorize]])
  (:refer-clojure :exclude [import]))

(s/defn save [{:keys [user-id]} {:keys [id master-id] :as dashboard} :- Dashboard]
  {:pre [(not master-id)]} ; Slaves can't be saved (no pun intented :-)
  (when id
    (authorize (= user-id (:owner-id (r/find-by-id db id)))))
  (r/save-dashboard db (assoc dashboard :owner-id user-id)))

(s/defn import [{:keys [user-id]} {:keys [master-id owner-id] :as dashboard} :- Dashboard]
  (let [m (r/find-by-id db master-id)]
    (authorize (and (some? m) (not= user-id owner-id))))
  (r/save-dashboard db (assoc dashboard :owner-id user-id)))

(defn delete [{:keys [user-id]} id]
  (authorize (= user-id (:owner-id (r/find-by-id db id))))
  (r/delete-dashboard db id))

(defn find-all [{:keys [user-id]}]
  (r/find-dashboards db user-id))

(s/defn find-by-id :- Dashboard [{:keys [user-id]} id]
  (r/find-with-relations db id))
