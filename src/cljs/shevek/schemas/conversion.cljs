(ns shevek.schemas.conversion
  (:require [shevek.dw :as dw]))

(defn- build-time-filter [{:keys [dimensions] :as cube}]
  (assoc (dw/time-dimension dimensions)
         :selected-period :latest-day))

(defn- build-new-viewer [{:keys [measures] :as cube}]
  {:cube cube
   :filter [(build-time-filter cube)]
   :split []
   :measures (->> measures (take 3) vec)
   :pinboard {:measure (first measures) :dimensions []}})

(defn- report-dim->viewer [{:keys [name selected-period sort-by value] :as dim}
                           {:keys [dimensions measures]}]
  (cond-> (merge dim (dw/find-dimension name dimensions))
          selected-period (update :selected-period keyword)
          value (update :value set)
          sort-by (update :sort-by merge (dw/find-dimension (:name sort-by) (concat dimensions measures)))))

(defn- report-dims->viewer [coll cube]
  (mapv #(report-dim->viewer % cube) coll))

(defn report->viewer [{:keys [pinboard] :as report} cube]
  {:cube cube
   :filter (report-dims->viewer (report :filter) cube)
   :split (report-dims->viewer (report :split) cube)
   :measures (mapv #(dw/find-dimension % (cube :measures)) (report :measures))
   :pinboard {:measure (dw/find-dimension (:measure pinboard) (cube :measures))
              :dimensions (report-dims->viewer (-> report :pinboard :dimensions) cube)}})

(defn- viewer-dim->report [{:keys [selected-period value sort-by] :as dim}]
  (cond-> (dissoc dim :type :title :description)
          selected-period (update :selected-period name)
          value (update :value vec)
          sort-by (update :sort-by viewer-dim->report)))

(defn viewer->report [{:keys [cube measures filter split pinboard]}]
  {:cube (:name cube)
   :measures (map :name measures)
   :filter (map viewer-dim->report filter)
   :split (map viewer-dim->report split)
   :pinboard {:measure (-> pinboard :measure :name)
              :dimensions (map viewer-dim->report (:dimensions pinboard))}})
