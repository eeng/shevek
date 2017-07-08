(ns shevek.schema.seed
  (:require [clojure.edn :as edn]
            [shevek.db :refer [db]]
            [shevek.users.repository :as users]
            [shevek.schema.manager :refer [update-cubes]]
            [shevek.app :refer [start-db]]
            [taoensso.timbre :as log]))

(defn create-users []
  (log/info "Seeding users...")
  (users/create-or-update-by db :username {:username "admin" :fullname "Administrator" :password "asdf654"}))

(defn create-cubes []
  (log/info "Seeding schema...")
  (let [{:keys [cubes]} (-> "seed-examples/vitolen.edn" slurp edn/read-string)]
    (update-cubes db cubes)))

(defn seed! []
  (start-db)
  (create-users)
  (create-cubes))

#_(seed!)
