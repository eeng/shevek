(ns shevek.reports.conversion
  (:require [shevek.schemas.report :refer [keys-to-remove-from-viewer]]))

(defn- simplify-dimension [dim]
  (apply dissoc dim keys-to-remove-from-viewer))

(defn viewer->report [{:keys [cube measures filter split]}]
  {:cube (:_id cube)
   :measures (map :name measures)
   :filter (map simplify-dimension filter)
   :split (map #(-> % simplify-dimension (assoc :sort-by (simplify-dimension (:sort-by %)))) split)})
