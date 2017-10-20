(ns shevek.schemas.conversion
  (:require [shevek.lib.dw.dims :refer [find-dimension time-dimension]]
            [shevek.lib.dates :refer [to-iso8601 parse-time]]
            [shevek.schemas.query :refer [Query RawQuery]]
            [schema-tools.core :as st]
            [com.rpl.specter :refer [setval ALL]]))

(defn- build-time-filter [{:keys [dimensions] :as cube}]
  (assoc (time-dimension dimensions) :period "latest-day"))

(defn- default-measures [{:keys [measures]}]
  (vec (if (some :favorite measures)
         (filter :favorite measures)
         (take 3 measures))))

(defn build-new-viewer [cube]
  (let [measures (default-measures cube)]
    {:cube cube
     :viztype :totals
     :filters [(build-time-filter cube)]
     :splits []
     :measures measures
     :pinboard {:measure (first measures) :dimensions []}}))

(defn- report-dim->viewer [{:keys [name period interval sort-by value] :as dim}
                           {:keys [dimensions measures]}]
  (cond-> (merge dim (find-dimension name dimensions))
          interval (update :interval (partial map parse-time))
          value (update :value set)
          sort-by (update :sort-by merge (find-dimension (:name sort-by) (concat dimensions measures)))))

(defn- report-dims->viewer [coll cube]
  (mapv #(report-dim->viewer % cube) coll))

(defn report->viewer [{:keys [pinboard viztype] :as report} cube]
  {:cube cube
   :viztype (keyword viztype)
   :filters (report-dims->viewer (report :filters) cube)
   :splits (report-dims->viewer (report :splits) cube)
   :measures (filterv some? (map #(find-dimension % (cube :measures)) (report :measures)))
   :pinboard {:measure (or (find-dimension (:measure pinboard) (cube :measures))
                           (first (default-measures cube)))
              :dimensions (report-dims->viewer (-> report :pinboard :dimensions) cube)}})

(defn- viewer-dim->report [{:keys [period interval value sort-by] :as dim}]
  (cond-> (select-keys dim [:name :period :interval :value :sort-by :descending :on :granularity :limit :operator :value])
          interval (update :interval (partial map to-iso8601))
          value (update :value vec)
          sort-by (update :sort-by viewer-dim->report)))

(defn viewer->report [{:keys [cube measures filters splits pinboard viztype]}]
  {:cube (:name cube)
   :viztype (when viztype (name viztype))
   :measures (map :name measures)
   :filters (map viewer-dim->report filters)
   :splits (map viewer-dim->report splits)
   :pinboard {:measure (-> pinboard :measure :name)
              :dimensions (map viewer-dim->report (:dimensions pinboard))}})

(defn viewer->query [{:keys [cube] :as viewer}]
  (-> (assoc viewer :cube (cube :name))
      (update :measures (partial map :name))
      (st/select-schema Query)))

(defn viewer->raw-query [{:keys [cube] :as viewer}]
  (-> (assoc viewer :cube (cube :name))
      (update :measures (partial map :name))
      (st/select-schema RawQuery)))
