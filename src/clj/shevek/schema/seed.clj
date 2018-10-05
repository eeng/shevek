(ns shevek.schema.seed
  (:require [shevek.users.repository :as users]
            [shevek.schema.manager :refer [update-cubes]]
            [taoensso.timbre :refer [debug]]
            [shevek.config :refer [config]]
            [shevek.db :as db]))

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

(defn db-reset!
  "DO NOT use on production!"
  [db]
  (db/clean! db)
  (seed! db))

#_(db-reset! shevek.db/db)
