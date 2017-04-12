(ns shevek.reports.conversion)

(defn- simplify-dimension [{:keys [selected-period] :as dim}]
  (cond-> (dissoc dim :type :title :description)
          selected-period (assoc :selected-period (name selected-period))))

(defn viewer->report [{:keys [cube measures filter split]}]
  {:cube (:_id cube)
   :measures (map :name measures)
   :filter (map simplify-dimension filter)
   :split (map #(-> % simplify-dimension (assoc :sort-by (simplify-dimension (:sort-by %)))) split)})
