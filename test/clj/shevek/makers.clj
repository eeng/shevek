(ns shevek.makers
  (:require [schema-generators.complete :as c]
            [shevek.schema.repository :refer [Cube save-cube]]
            [shevek.db :refer [db]]))

(defn make [schema args]
  (c/complete args schema))

(def makers {Cube save-cube})

(defn make! [schema args]
  (->> (c/complete args schema)
       ((makers schema) db)))
