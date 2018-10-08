(ns shevek.schema.api
  (:require [shevek.schema.repository :as r]
            [shevek.db :refer [db]]
            [shevek.engine.state :refer [dw]]
            [shevek.engine.protocol :as e]
            [shevek.schema.auth :as auth]))

(defn cubes [{:keys [user]}]
  (->> (r/find-cubes db)
       (auth/filter-cubes user)
       (map #(dissoc % :created-at :updated-at))
       (sort-by :title)))

(defn cube [{:keys [user] :as req} name]
  (-> (r/find-cube db name)
      (auth/filter-cube user)
      (dissoc :min-time :updated-at :created-at)))

(defn save-cube [_ cube]
  (r/save-cube db cube))

(defn max-time [_ cube-name]
  (:max-time (e/time-boundary dw cube-name)))

;; Examples

#_(cubes {:user {:permissions {:allowed-cubes "all"}}})
#_(cube nil "vtol_stats")
