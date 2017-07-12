(ns shevek.schema.api
  (:require [shevek.schema.repository :as r]
            [shevek.schema.metadata :as m]
            [shevek.db :refer [db]]
            [shevek.dw :refer [dw]]))

(defn cubes [_]
  (r/find-cubes db))

(defn max-time [_ cube-name]
  (:max-time (m/time-boundary dw cube-name)))

(defn cube [req name]
  (assoc (r/find-cube db name)
         :max-time (max-time req name)))

(defn save-cube [_ cube]
  (r/save-cube db cube))

;; Examples

#_(cubes)
#_(cube "wikiticker")
