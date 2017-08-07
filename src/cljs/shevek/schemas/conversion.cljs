(ns shevek.schemas.conversion
  (:require [shevek.lib.dw.dims :refer [find-dimension time-dimension time-dimension?]]
            [shevek.lib.dw.time :refer [effective-interval]]
            [shevek.lib.dates :refer [to-iso8601 end-of-day parse-time]]
            [shevek.schemas.query :refer [Query RawQuery]]
            [schema-tools.core :as st]
            [com.rpl.specter :refer [setval ALL]]))

(defn- build-time-filter [{:keys [dimensions] :as cube}]
  (assoc (time-dimension dimensions)
         :period :latest-day))

(defn build-new-viewer [{:keys [measures] :as cube}]
  (let [measures (if (some :favorite measures)
                  (filter :favorite measures)
                  (take 3 measures))]
    {:cube cube
     :viztype "totals"
     :filter [(build-time-filter cube)]
     :split []
     :measures (vec measures)
     :pinboard {:measure (first measures) :dimensions []}}))

(defn- report-dim->viewer [{:keys [name period interval sort-by value] :as dim}
                           {:keys [dimensions measures]}]
  (cond-> (merge dim (find-dimension name dimensions))
          period (update :period keyword)
          interval (update :interval (partial map parse-time))
          value (update :value set)
          sort-by (update :sort-by merge (find-dimension (:name sort-by) (concat dimensions measures)))))

(defn- report-dims->viewer [coll cube]
  (mapv #(report-dim->viewer % cube) coll))

(defn report->viewer [{:keys [pinboard] :as report} cube]
  {:cube cube
   :filter (report-dims->viewer (report :filter) cube)
   :split (report-dims->viewer (report :split) cube)
   :measures (mapv #(find-dimension % (cube :measures)) (report :measures))
   :pinboard {:measure (find-dimension (:measure pinboard) (cube :measures))
              :dimensions (report-dims->viewer (-> report :pinboard :dimensions) cube)}})

(defn- viewer-dim->report [{:keys [period interval value sort-by] :as dim}]
  (cond-> (dissoc dim :type :title :description :format :expression :favorite)
          period (update :period name)
          interval (update :interval (partial map to-iso8601))
          value (update :value vec)
          sort-by (update :sort-by viewer-dim->report)))

(defn viewer->report [{:keys [cube measures filter split pinboard]}]
  {:cube (:name cube)
   :measures (map :name measures)
   :filter (map viewer-dim->report filter)
   :split (map viewer-dim->report split)
   :pinboard {:measure (-> pinboard :measure :name)
              :dimensions (map viewer-dim->report (:dimensions pinboard))}})

(defn- add-str-interval [viewer]
  (setval [:filter ALL time-dimension? :interval]
          (mapv to-iso8601 (effective-interval viewer))
          viewer))

(defn viewer->query [{:keys [cube] :as viewer}]
  (-> (add-str-interval viewer)
      (assoc :cube (cube :name))
      (st/select-schema Query)))

(defn viewer->raw-query [{:keys [cube] :as viewer}]
  (-> (add-str-interval viewer)
      (assoc :cube (cube :name))
      (st/select-schema RawQuery)))

(defn report->query [report cube]
  (-> report (report->viewer cube) (assoc :totals true) viewer->query))
