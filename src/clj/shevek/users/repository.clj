(ns shevek.users.repository
  (:require [schema.core :as s]
            [shevek.lib.mongodb :as m]
            [bcrypt-clj.auth :refer [crypt-password]]
            [shevek.schemas.user :refer [User]]
            [shevek.reports.repository :refer [delete-reports]]
            [shevek.dashboards.repository :refer [delete-dashboards]]
            [clj-time.core :refer [now]]))

(defn find-users [db]
  (m/find-all db "users" :fields [:username :fullname :email :admin :allowed-cubes] :sort {:username 1}))

(defn find-by [db condition]
  (m/find-by db "users" condition))

(defn find-by-username [db username]
  (find-by db {:username username}))

(defn- encrypt-password [{:keys [password] :as user}]
  (if (seq password)
    (assoc user :password (crypt-password password))
    (dissoc user :password)))

(defn find-by-id [db id]
  (m/find-by-id db "users" id))

(defn reload [db {:keys [id]}]
  (find-by-id db id))

(defn ensure-admin-permissions [{:keys [admin] :as user}]
  (cond-> user
          admin (assoc :allowed-cubes "all")))

(defn create-or-update-by [db field user]
  (let [value (field user)
        existing (or (and value (find-by db {field value})) {})
        merged (->> (encrypt-password user)
                    (ensure-admin-permissions)
                    (merge existing))]
    (s/validate User merged)
    (m/save db "users" merged)))

(defn save-user [db user]
  (create-or-update-by db :id user))

(defn delete-user [db id]
  (m/delete-by-id db "users" id)
  (delete-reports db id)
  (delete-dashboards db id))

(defn update-sign-in-timestamps [db {:keys [id last-sign-in-at] :as user}]
  (m/update-by-id db "users" id {:previous-sign-in-at last-sign-in-at :last-sign-in-at (now)})
  user)

;; Examples

#_(save-user shevek.db/db {:username "dev" :fullname "Desarrollo" :password "prueba" :admin true})
#_(find-users shevek.db/db)
