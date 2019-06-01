(ns shevek.reports.repository
  (:require [shevek.lib.mongodb :as m]
            [monger.collection :as mc]
            [monger.operators :refer [$exists $lt]]
            [clj-time.core :as t]))

(defn save-report [db report]
  (m/save db "reports" report))

(defn delete-report [db id]
  (m/delete-by-id db "reports" id))

(defn find-reports [db user-id]
  (m/find-all db "reports" :where {:owner-id user-id} :sort {:name 1}))

(defn delete-reports [db user-id]
  (m/delete-by db "reports" {:owner-id user-id}))

(defn find-by-id! [db id]
  (m/find-by-id! db "reports" id))

(defn create-or-update-by-sharing-digest [db {:keys [sharing-digest] :as report}]
  {:pre [sharing-digest]}
  (m/create-or-update-by db "reports" {:sharing-digest sharing-digest} report))

(defn delete-old-shared-reports [db & [{:keys [older-than] :or {older-than (-> 30 t/days t/ago)}}]]
  (mc/remove db "reports" {:sharing-digest {$exists true}
                           :updated-at {$lt older-than}}))

#_(delete-old-shared-reports shevek.db/db {:older-than (t/now)})
