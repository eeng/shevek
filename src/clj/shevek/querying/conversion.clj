(ns shevek.querying.conversion
  (:require [clojure.string :as str]
            [shevek.lib.collections :refer [assoc-if-seq]]))

; TODO repetida en el client
(defn dim=? [dim1 dim2]
  (= (:name dim1) (:name dim2)))

; TODO repetida en el client
(defn includes-dim? [coll dim]
  (some #(dim=? % dim) coll))

; TODO repetida en el client
(defn time-dimension? [{:keys [name interval]}]
  (or (= name "__time") interval))

(defn- to-druid-agg [{:keys [name type] :or {type "doubleSum"}}]
  {:fieldName name :name name :type type})

(defn- sort-by-same? [{:keys [name sort-by]}]
  (= name (:name sort-by)))

(defn- add-sort-by-dim-to-aggregations [{:keys [sort-by] :as dim} measures aggregations]
  "If we sort by a not selected metric we should send the field as an aggregation, otherwise Druid complains"
  (if (and (:name sort-by) (not (sort-by-same? dim)) (not (includes-dim? measures sort-by)))
    (conj aggregations (to-druid-agg sort-by))
    aggregations))

(defn- to-druid-filter [[{:keys [name operator value]} :as filters]]
  (condp = (count filters)
    0 nil
    1 (condp = (keyword operator)
        :is {:type "selector" :dimension name :value value}
        :include {:type "in" :dimension name :values value}
        :exclude {:type "not" :field {:type "in" :dimension name :values value}}
        :search {:type "search" :dimension name :query {:type "insensitive_contains" :value value}})
    {:type "and" :fields (map #(to-druid-filter [%]) filters)}))

(defn- calculate-query-type [{:keys [dimension]}]
  (if (and dimension (not (time-dimension? dimension)))
    "topN"
    "timeseries"))

(defn- generate-metric-field [{:keys [name sort-by] :as dim} measures]
  (let [descending (or (nil? (:descending sort-by)) (:descending sort-by))
        field (if (sort-by-same? dim)
                {:type "dimension" :ordering "lexicographic"}
                {:type "numeric" :metric (or (:name sort-by) (-> measures first :name))})]
    (if (or (and (sort-by-same? dim) (not descending))
            (and (not (sort-by-same? dim)) descending))
      field
      {:type "inverted" :metric field})))

; TODO el threshold en las timeseries query no existe, quizas habria que limitar en mem nomas
(defn- add-query-type-dependant-fields [{:keys [dimension measures] :as q}
                                        {:keys [queryType] :as dq}]
  (condp = queryType
    "topN"
    (assoc dq
           :granularity {:type "all"}
           :dimension (dimension :name)
           :metric (generate-metric-field dimension measures)
           :threshold (dimension :limit (or 100)))
    "timeseries"
    (assoc dq
           :granularity (if dimension
                          {:type "period" :period (:granularity dimension)}
                          {:type "all"})
           :descending (get-in dimension [:sort-by :descending] false))))

(defn- with-value? [{:keys [operator value]}]
  (or (= "is" operator) (seq value)))

(defn to-druid-query [{:keys [cube measures dimension] :as q}]
  (let [[time-filters normal-filters] ((juxt filter remove) time-dimension? (:filter q))]
    (as-> {:queryType (calculate-query-type q)
           :dataSource {:type "table" :name cube}
           :intervals (str/join "/" (-> time-filters first :interval))
           :aggregations (->> measures (mapv to-druid-agg) (add-sort-by-dim-to-aggregations dimension measures))}
          dq
          (add-query-type-dependant-fields q dq)
          (assoc-if-seq dq :filter (->> normal-filters (filter with-value?) to-druid-filter)))))

(defn from-druid-results [{:keys [dimension]} {:keys [queryType]} results]
  (condp = queryType
    "topN" (-> results first :result)
    "timeseries" (map (fn [{:keys [result timestamp]}]
                        (if dimension
                          (assoc result :__time timestamp)
                          result))
                      results)))

(defn make-tig
  "tig = Temporary ID Generator, counter for generating temporary field names used in aggregations that are later refered in post-aggregations"
  []
  (let [counter (atom -1)]
    (fn []
      (swap! counter inc))))
