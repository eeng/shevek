(ns shevek.schema.repository
  (:require [schema.core :as s]
            [monger.collection :as mc]
            [shevek.schemas.cube :refer [Cube]]))

(s/defn save-cube [db cube :- Cube]
  (mc/save-and-return db "cubes" cube))

(defn find-cubes [db]
  (mc/find-maps db "cubes"))

(defn find-cube [db name]
  (mc/find-one-as-map db "cubes" {:name name}))
