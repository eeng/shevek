(ns shevek.reports.api
  (:require [shevek.reports.repository :as r]
            [shevek.db :refer [db]]
            [schema.core :as s]
            [shevek.schemas.report :refer [Report]]
            [shevek.lib.sharing :refer [base-url hash-data]]
            [shevek.lib.auth :refer [authorize-to-owner]]))

(s/defn save [{:keys [user-id] :as req} {:keys [id] :as report} :- Report]
  (when id
    (authorize-to-owner req (r/find-by-id! db id)))
  (r/save-report db (assoc report :owner-id user-id)))

(defn delete [req id]
  (authorize-to-owner req (r/find-by-id! db id))
  (r/delete-report db id))

(defn find-all [{:keys [user-id]}]
  (r/find-reports db user-id))

(defn- shared? [{:keys [sharing-digest]}]
  (some? sharing-digest))

(defn find-by-id [_ id]
  (let [report (r/find-by-id! db id)]
    (if (shared? report)
      (dissoc report :id :shared-by-id :sharing-digest)
      report)))

(defn- create-shared-report [report user-id]
  (let [structure (select-keys report [:name :cube :viztype :measures :filters :splits :pinboard])]
    (r/create-or-update-by-sharing-digest
     db
     (assoc (dissoc report :id :owner-id)
            :shared-by-id user-id
            :sharing-digest (hash-data structure)))))

(defn share-url [{:keys [user-id] :as request} report]
  (let [{:keys [id]} (create-shared-report report user-id)]
    (str (base-url request) "/reports/" id)))
