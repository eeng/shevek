(ns shevek.schema.seed
  (:require [clojure.edn :as edn]
            [shevek.schema.manager :refer [update-cubes]]))

(defn seed! [db filepath]
  (let [{:keys [cubes]} (-> filepath slurp edn/read-string)]
    (update-cubes db cubes)))

#_(seed! shevek.db/db "seed-examples/vitolen.edn")
