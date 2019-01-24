(ns shevek.schema.seed
  (:require [shevek.users.repository :as users]
            [shevek.schema.manager :as m]
            [taoensso.timbre :refer [debug]]
            [shevek.config :refer [config]]
            [shevek.db :as db]
            [shevek.engine.state :refer [dw]]))

(defn users [db]
  (when-not (users/find-by db {:admin true})
    (debug "Seeding admin user.")
    (users/create-or-update-by db :username {:username "admin" :fullname "Administrator" :password "asdf654" :admin true})))

(defn cubes [db]
  (debug "Seeding schema.")
  (m/update-cubes db (config :cubes)))

(defn seed! [db]
  (users db)
  (cubes db))

(defn db-reset!
  "DO NOT use on production!"
  [db]
  (db/clean! db)
  (m/discover! dw db)
  (seed! db))
