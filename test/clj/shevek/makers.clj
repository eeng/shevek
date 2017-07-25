(ns shevek.makers
  (:require [schema-generators.complete :as c]
            [shevek.schema.repository :refer [save-cube]]
            [shevek.schemas.cube :refer [Cube]]
            [shevek.schemas.user :refer [User]]
            [shevek.users.repository :refer [save-user]]
            [shevek.db :refer [db]]))

(def makers {Cube save-cube
             User save-user})

(defn make [schema args]
  (c/complete args schema))

(defn make!
  ([schema] (make! schema {}))
  ([schema args]
   (let [saver (makers schema)]
     (assert saver (str "You need to define a save function (in makers) for the schema: " (meta schema)))
     (->> (make schema args) (saver db)))))
