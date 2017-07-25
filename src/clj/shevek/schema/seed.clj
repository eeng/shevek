(ns shevek.schema.seed
  (:require [shevek.users.repository :as users]
            [shevek.schema.manager :refer [update-cubes]]
            [taoensso.timbre :refer [debug]]
            [cprop.core :refer [load-config]]))

(defn users [db]
  (when-not (users/find-by-username db "admin")
    (debug "Seeding admin user.")
    (users/save-user db {:username "admin" :fullname "Administrator" :password "asdf654" :admin true})))

(defn cubes [db]
  (debug "Seeding schema.")
  (let [{:keys [cubes]} (load-config :file "resources/config.edn")]
    (update-cubes db cubes)))

(defn seed! [db]
  (users db)
  (cubes db))

#_(seed! shevek.db/db)
