(ns shevek.schemas.conversion
  (:require [shevek.domain.dimension :refer [find-dimension time-dimension]]
            [shevek.lib.time :refer [to-iso8601 parse-time end-of-day]]
            [shevek.schemas.query :refer [Query RawQuery]]
            [shevek.schemas.app-db :refer [CurrentReport]]
            [schema-tools.core :as st]
            [com.rpl.specter :refer [transform must ALL]]))

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

(defn- report-dim->viewer [{:keys [name sort-by] :as dim}
                           {:keys [dimensions measures]}]
  (->> (merge dim (find-dimension name dimensions))
       (transform [(must :interval) ALL] parse-time)
       (transform (must :value) set)
       (transform (must :sort-by) #(merge % (find-dimension (:name sort-by) (concat dimensions measures))))))

(defn report-dims->viewer [coll cube]
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

(defn stringify-interval [[from to]]
  (map to-iso8601 [from (end-of-day to)]))

(defn unparse-filters [filters]
  (->> filters
       (transform [ALL (must :interval)] stringify-interval)
       (transform [ALL (must :value) set?] vec)))

(defn- simplify-viewer [viewer]
  (->> viewer
       (transform :cube :name)
       (transform [:measures ALL] :name)
       (transform :filters unparse-filters)))

(defn viewer->report [viewer]
  (as-> (simplify-viewer viewer) q
        (transform :viztype name q)
        (transform [:pinboard :measure] :name q)
        (st/select-schema q CurrentReport)))

(defn viewer->query [viewer]
  (-> (simplify-viewer viewer)
      (st/select-schema Query)))

(defn viewer->raw-query [viewer]
  (-> (simplify-viewer viewer)
      (st/select-schema RawQuery)))
