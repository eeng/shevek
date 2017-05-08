(ns shevek.schemas.conversion
  (:require [shevek.dw :as dw]
            [shevek.lib.dates :refer [to-iso8601 end-of-day format-date parse-time]]
            [shevek.schemas.query :refer [Query]]
            [schema-tools.core :as st]
            [com.rpl.specter :refer [setval ALL]]))

(defn- build-time-filter [{:keys [dimensions] :as cube}]
  (assoc (dw/time-dimension dimensions)
         :period :latest-day))

(defn- build-new-viewer [{:keys [measures] :as cube}]
  {:cube cube
   :filter [(build-time-filter cube)]
   :split []
   :measures (->> measures (take 3) vec)
   :pinboard {:measure (first measures) :dimensions []}})

(defn- report-dim->viewer [{:keys [name period interval sort-by value] :as dim}
                           {:keys [dimensions measures]}]
  (cond-> (merge dim (dw/find-dimension name dimensions))
          period (update :period keyword)
          interval (update :interval (partial map parse-time))
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

(defn- viewer-dim->report [{:keys [period interval value sort-by] :as dim}]
  (cond-> (dissoc dim :type :title :description :format :expression)
          period (update :period name)
          interval (update :interval (partial map format-date))
          value (update :value vec)
          sort-by (update :sort-by viewer-dim->report)))

(defn viewer->report [{:keys [cube measures filter split pinboard]}]
  {:cube (:name cube)
   :measures (map :name measures)
   :filter (map viewer-dim->report filter)
   :split (map viewer-dim->report split)
   :pinboard {:measure (-> pinboard :measure :name)
              :dimensions (map viewer-dim->report (:dimensions pinboard))}})

(defn- add-interval [{:keys [filter] :as q} max-time]
  (let [{:keys [period interval]} (dw/time-dimension filter)
        interval (when interval [(first interval) (end-of-day (second interval))])]
    (setval [:filter ALL dw/time-dimension? :interval]
            (mapv to-iso8601 (if period (dw/to-interval period max-time) interval))
            q)))

(defn viewer->query [{:keys [cube] :as viewer}]
  (-> (add-interval viewer (cube :max-time))
      (assoc :cube (cube :name))
      (st/select-schema Query)))

(defn report->query [report cube]
  (-> report (report->viewer cube) (assoc :totals true) viewer->query))
