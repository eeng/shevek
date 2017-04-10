(ns shevek.users.repository
  (:require [schema.core :as s]
            [monger.collection :as mc]
            [monger.query :as mq]
            [bcrypt-clj.auth :refer [crypt-password]]))

(s/defschema User
  {:username s/Str
   :fullname s/Str
   :password (s/constrained s/Str (comp pos? count))
   (s/optional-key :email) s/Str
   (s/optional-key :_id) s/Any})

(defn- encrypt-password [{:keys [password] :as user}]
  (if (seq password)
    (assoc user :password (crypt-password password))
    (dissoc user :password)))

(defn reload [db {:keys [_id]}]
  (mc/find-map-by-id db "users" _id))

(defn save-user [db {:keys [_id] :as user}]
  (let [existing (if _id (reload db user) {})
        merged (merge existing (encrypt-password user))]
    (s/validate User merged)
    (mc/save-and-return db "users" merged)))

(s/defn delete-user [db {:keys [_id]} :- User]
  (mc/remove-by-id db "users" _id)
  true)

(defn find-users [db]
  (mq/with-collection db "users"
    (mq/sort {:username 1})))

#_(save-user shevek.db/db {:username "ddchp" :fullname "Daniel Chavarini" :password "prueba" :email "auditor@vitolen.com"})
#_(save-user shevek.db/db {:username "dev" :fullname "Desarrollo" :password "prueba"})
