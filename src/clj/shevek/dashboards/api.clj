(ns shevek.dashboards.api
  (:require [schema.core :as s]
            [schema-tools.core :as st]
            [shevek.schemas.dashboard :refer [Dashboard]]
            [shevek.dashboards.repository :as r]
            [shevek.db :refer [db]]
            [shevek.lib.auth :refer [authorize-to-owner]]
            [com.rpl.specter :refer [transform ALL]])
  (:refer-clojure :exclude [import]))

(s/defn save [{:keys [user-id] :as req} {:keys [id master-id] :as dashboard} :- Dashboard]
  {:pre [(not master-id)]} ; Slaves can't be saved (no pun intented :-)
  (when id
    (authorize-to-owner req (r/find-by-id db id)))
  (r/save-dashboard db (assoc dashboard :owner-id user-id)))

(defn delete [req id]
  (authorize-to-owner req (r/find-by-id db id))
  (r/delete-dashboard db id))

(defn find-all [{:keys [user-id]}]
  (r/find-dashboards db user-id))

(s/defn find-by-id :- Dashboard [{:keys [user-id]} id]
  (r/find-with-relations db id))

(s/defschema ImportRequest
  (-> (st/dissoc Dashboard :id)
      (st/assoc :original-id s/Str
                :import-as (s/enum "link" "copy"))))

(s/defn import [{:keys [user-id]} {:keys [import-as original-id owner-id] :as data} :- ImportRequest]
  {:pre [(not= user-id owner-id)]} ; You can't import your own dashboards
  (let [original (r/find-with-relations db original-id)
        imported (-> (dissoc data :original-id :import-as)
                     (assoc :owner-id user-id))]
    (assert original) ; TODO Here we should raise some application error instead that shouldn't be logged with a type so we can translated on the client and show a modal
    (as-> imported i
          (case import-as
            "link" (assoc i :master-id original-id :panels [])
            "copy" (assoc i :panels (transform [ALL :report] #(dissoc % :id) (:panels original))))
          (r/save-dashboard db i)
          (select-keys i [:id]))))

(defn receive-report [req {:keys [report dashboard-id]}]
  (let [dashboard (r/find-by-id db dashboard-id)
        report (dissoc report :id :owner-id :dashboard-id)]
    (authorize-to-owner req dashboard)
    (->> (update dashboard :panels conj {:type "report" :report report})
         (r/save-dashboard db))))
