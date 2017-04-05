(ns shevek.schema.api
  (:require [shevek.schema.repository :as r]
            [shevek.schema.metadata :as m]
            [shevek.db :refer [db]]
            [shevek.dw :refer [dw]]))

(defn cubes []
  (r/find-cubes db))

(defn max-time [cube-name]
  (:max-time (m/time-boundary dw cube-name)))

(defn cube [name]
  (assoc (r/find-cube db name)
         :max-time (max-time name)))

;; Examples

#_(cubes)
#_(cube "wikiticker")
