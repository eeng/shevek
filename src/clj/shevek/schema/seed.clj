(ns shevek.schema.seed
  (:require [shevek.users.repository :as users]
            [shevek.schema.manager :refer [update-cubes]]
            [taoensso.timbre :refer [debug]]
            [shevek.config :refer [config]]))

(defn users [db]
  (when-not (users/find-by db {:admin true})
    (debug "Seeding admin user.")
    (users/create-or-update-by db :username {:username "admin" :fullname "Administrator" :password "asdf654" :admin true})))

(defn cubes [db]
  (debug "Seeding schema.")
  (update-cubes db (config :cubes)))

(defn seed! [db]
  (users db)
  (cubes db))

#_(seed! shevek.db/db)
