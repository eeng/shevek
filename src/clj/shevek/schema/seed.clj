(ns shevek.schema.seed
  (:require [clojure.edn :as edn]
            [shevek.users.repository :as users]
            [shevek.schema.manager :refer [update-cubes]]
            [taoensso.timbre :refer [debug]]))

(defn users [db]
  (when-not (users/find-by-username db "admin")
    (debug "Seeding admin user.")
    (users/save-user db {:username "admin" :fullname "Administrator" :password "asdf654"})))

(defn cubes [db]
  (debug "Seeding schema.")
  (let [{:keys [cubes]} (-> "seed-examples/vitolen.edn" slurp edn/read-string)]
    (update-cubes db cubes)))

(defn seed! [db]
  (users db)
  (cubes db))

#_(seed! shevek.db/db)
