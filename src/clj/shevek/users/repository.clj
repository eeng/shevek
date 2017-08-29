(ns shevek.users.repository
  (:require [schema.core :as s]
            [monger.collection :as mc]
            [monger.query :as mq]
            [bcrypt-clj.auth :refer [crypt-password]]
            [shevek.schemas.user :refer [User]]))

(defn find-users [db]
  (mq/with-collection db "users"
    (mq/fields [:username :fullname :email :admin :permissions])
    (mq/sort {:username 1})))

(defn find-by-username [db username]
  (mc/find-one-as-map db "users" {:username username}))

(defn- encrypt-password [{:keys [password] :as user}]
  (if (seq password)
    (assoc user :password (crypt-password password))
    (dissoc user :password)))

(defn find-by-id [db id]
  (mc/find-map-by-id db "users" id))

(defn reload [db {:keys [_id]}]
  (find-by-id db _id))

(defn find-by [db fields-and-values]
  (mc/find-one-as-map db "users" fields-and-values))

(defn ensure-admin-permissions [{:keys [admin] :as user}]
  (cond-> user
          admin (assoc-in [:permissions :allowed-cubes] "all")))

(defn create-or-update-by [db field user]
  (let [value (field user)
        existing (or (and value (find-by db {field value})) {})
        merged (->> (encrypt-password user)
                    (ensure-admin-permissions)
                    (merge existing))]
    (s/validate User merged)
    (mc/save-and-return db "users" merged)))

(defn save-user [db user]
  (create-or-update-by db :_id user))

(defn delete-user [db {:keys [_id]}]
  (mc/remove-by-id db "users" _id)
  true)

;; Examples

#_(save-user shevek.db/db {:username "ddchp" :fullname "Daniel Chavarini" :password "prueba" :email "auditor@vitolen.com"})
#_(save-user shevek.db/db {:username "dev" :fullname "Desarrollo" :password "prueba"})
#_(find-users shevek.db/db)
