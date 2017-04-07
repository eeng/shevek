(ns shevek.users.repository
  (:require [schema.core :as s]
            [monger.collection :as mc]))

(s/defschema User
  {:username s/Str
   :fullname s/Str
   :password s/Str
   (s/optional-key :email) s/Str})

(s/defn save-user [db user :- User]
  (mc/save-and-return db "users" user))

#_(save-user shevek.db/db {:username "ddchp" :fullname "Daniel Chavarini" :password "prueba"})
