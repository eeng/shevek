(ns shevek.reports.conversion)

(defn- simplify-sort-by [{:keys [dimension measure] :as sort-by}]
  (cond-> sort-by
          dimension (assoc :dimension (:name dimension))
          measure (assoc :measure (:name measure))))

(defn viewer->report [{:keys [cube measures filter split]}]
  {:cube (:_id cube)
   :measures (map :name measures)
   :filter (map #(assoc % :dimension (-> % :dimension :name)) filter)
   :split (map #(assoc % :dimension (-> % :dimension :name)
                         :sort-by (simplify-sort-by (:sort-by %)))
               split)})
