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
    (users/create-or-update-by db :username {:username "admin" :fullname "Administrator"
                                             :password "secret123" :admin true})))

(defn cubes [db dw]
  (debug "Seeding schema.")
  (let [discover? (pos? (config :datasources-discovery-interval))]
    (m/seed-schema! db dw {:discover? discover?})))

(defn seed! [db dw]
  (users db)
  (cubes db dw))

(defn db-reset!
  "DO NOT use on production!"
  [db]
  (db/clean! db)
  (seed! db dw))
