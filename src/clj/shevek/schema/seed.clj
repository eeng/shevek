(ns shevek.schema.seed
  (:require [clojure.edn :as edn]
            [shevek.db :refer [db]]
            [shevek.users.repository :as users]
            [shevek.schema.manager :refer [update-cubes]]))

(defn create-users []
  (users/create-or-update-by db :username {:username "admin" :fullname "Desarrollo" :password "admin321"}))

(defn create-cubes []
  (let [{:keys [cubes]} (-> "seed-examples/vitolen.edn" slurp edn/read-string)]
    (update-cubes db cubes)))

(defn seed! []
  (create-users)
  (create-cubes))

#_(seed!)
