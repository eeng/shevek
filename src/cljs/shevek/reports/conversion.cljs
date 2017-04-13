(ns shevek.reports.conversion
  (:require [shevek.dw :as dw]))

(defn- viewer-dim->report [{:keys [selected-period] :as dim}]
  (cond-> (dissoc dim :type :title :description)
          selected-period (assoc :selected-period (name selected-period))))

(defn viewer->report [{:keys [cube measures filter split]}]
  {:cube (:name cube)
   :measures (map :name measures)
   :filter (map viewer-dim->report filter)
   :split (map #(-> % viewer-dim->report (assoc :sort-by (viewer-dim->report (:sort-by %)))) split)})

(defn- report-dim->viewer [{:keys [name selected-period sort-by] :as dim} {:keys [dimensions measures]}]
  (cond-> (merge dim (dw/find-dimension name dimensions))
          selected-period (update :selected-period keyword)
          sort-by (update :sort-by merge (dw/find-dimension (:name sort-by) (concat dimensions measures)))))

(defn report->viewer [report cube]
  {:cube cube
   :filter (mapv #(report-dim->viewer % cube) (report :filter))
   :split (mapv #(report-dim->viewer % cube) (report :split))
   :measures (mapv #(dw/find-dimension % (cube :measures)) (report :measures))})
