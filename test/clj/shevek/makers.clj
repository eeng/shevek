(ns shevek.makers
  (:require [schema-generators.complete :as c]
            [shevek.schema.repository :refer [Cube save-cube]]
            [shevek.users.repository :refer [User save-user]]
            [shevek.db :refer [db]]))

(def makers {Cube save-cube
             User save-user})

(defn make [schema args]
  (c/complete args schema))

(defn make! [schema args]
  (let [saver (makers schema)]
    (assert saver (str "Maker not defined " (meta schema)))
    (->> (c/complete args schema)
         (saver db))))
