(ns shevek.reports.conversion)

(defn- simplify-dimension [dim]
  (dissoc dim :type :title :description))

(defn viewer->report [{:keys [cube measures filter split]}]
  {:cube (:_id cube)
   :measures (map :name measures)
   :filter (map simplify-dimension filter)
   :split (map #(-> % simplify-dimension (assoc :sort-by (simplify-dimension (:sort-by %)))) split)})
