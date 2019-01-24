(ns shevek.schemas.conversion
  (:require [shevek.domain.dimension :refer [find-dimension time-dimension]]
            [shevek.lib.time :refer [to-iso8601 parse-time end-of-day]]
            [shevek.schemas.query :refer [Query RawQuery]]
            [shevek.schemas.report :refer [Report]]
            [schema-tools.core :as st]
            [com.rpl.specter :refer [transform must ALL]]))

(defn- default-measures [{:keys [measures]}]
  (vec (if (some :favorite measures)
         (filter :favorite measures)
         (take 3 measures))))

(defn clean-sort-by [dim]
  (select-keys dim [:descending :name]))

(defn- report-dim->designer [{:keys [name sort-by] :as dim}
                             {:keys [dimensions measures]}]
  (when-let [full-dim (find-dimension name dimensions)]
    (->> (merge dim full-dim)
         (transform [(must :interval) ALL] parse-time)
         (transform (must :value) set)
         (transform (must :sort-by) #(clean-sort-by (merge % (find-dimension (:name sort-by) (concat dimensions measures))))))))

(defn report-dims->designer [coll cube]
  (vec (remove nil? (map #(report-dim->designer % cube) coll))))

(defn report->designer [{:keys [pinboard viztype] :as report} cube]
  {:viztype (keyword viztype)
   :filters (report-dims->designer (report :filters) cube)
   :splits (report-dims->designer (report :splits) cube)
   :measures (filterv some? (map #(find-dimension % (:measures cube)) (report :measures)))
   :pinboard {:measure (or (find-dimension (:measure pinboard) (:measures cube))
                           (first (default-measures cube)))
              :dimensions (report-dims->designer (-> report :pinboard :dimensions) cube)}})

(defn stringify-interval [[from to]]
  (map to-iso8601 [from (end-of-day to)]))

(defn unparse-filters [filters]
  (->> filters
       (transform [ALL (must :interval)] stringify-interval)
       (transform [ALL (must :value) set?] vec)))

(defn simplify-designer [designer]
  (->> (merge {:cube (get-in designer [:report :cube])} designer)
       (transform [:measures ALL] :name)
       (transform :filters unparse-filters)))

(defn designer->report [{:keys [report] :as designer}]
  (as-> (simplify-designer designer) d
        (transform :viztype name d)
        (transform [:pinboard :measure] :name d)
        (st/select-schema d Report)
        (merge report d)))

(defn unexpand [q]
  (-> (simplify-designer q)
      (st/select-schema Query)))

(defn designer->raw-query [designer]
  (-> (simplify-designer designer)
      (st/select-schema RawQuery)))
