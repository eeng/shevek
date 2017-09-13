(ns shevek.schema.repository
  (:require [schema.core :as s]
            [shevek.lib.mongodb :as m]
            [shevek.schemas.cube :refer [Cube]]))

(s/defn save-cube [db cube :- Cube]
  (m/save db "cubes" cube))

(defn find-cubes [db]
  (m/find-all db "cubes"))

(defn find-cube [db name]
  (m/find-by db "cubes" {:name name}))
