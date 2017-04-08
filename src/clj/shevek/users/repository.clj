(ns shevek.users.repository
  (:require [schema.core :as s]
            [monger.collection :as mc]
            [monger.query :as mq]))

(s/defschema User
  {:username s/Str
   :fullname s/Str
   :password s/Str
   (s/optional-key :email) s/Str
   (s/optional-key :_id) s/Any})

(s/defn save-user [db user :- User]
  (mc/save-and-return db "users" user))

(s/defn delete-user [db {:keys [_id]} :- User]
  (mc/remove-by-id db "users" _id)
  true)

(defn find-users [db]
  (mq/with-collection db "users"
    (mq/sort {:username 1})))

#_(save-user shevek.db/db {:username "ddchp" :fullname "Daniel Chavarini" :password "prueba" :email "auditor@vitolen.com"})
#_(save-user shevek.db/db {:username "dev" :fullname "Desarrollo" :password "prueba"})
