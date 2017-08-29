(ns shevek.schema.api
  (:require [shevek.schema.repository :as r]
            [shevek.schema.metadata :as m]
            [shevek.db :refer [db]]
            [shevek.dw :refer [dw]]
            [shevek.schema.auth :refer [filter-visible-cubes]]))

(defn cubes [{:keys [user]}]
  (->> (r/find-cubes db)
       (filter-visible-cubes (:permissions user))
       (map #(select-keys % [:_id :name :title :description]))
       (sort-by :title)))

(defn max-time [_ cube-name]
  (:max-time (m/time-boundary dw cube-name)))

(defn cube [req name]
  (assoc (r/find-cube db name)
         :max-time (max-time req name)))

(defn save-cube [_ cube]
  (r/save-cube db cube))

;; Examples

#_(cubes {:user {:permissions {:allowed-cubes "all"}}})
#_(cube nil "vtol_stats")
