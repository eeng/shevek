(ns shevek.querying.conversion
  (:require [clojure.string :as str]
            [shevek.lib.collections :refer [assoc-if-seq]]
            [shevek.querying.expression :refer [measure->druid]]
            [shevek.lib.dw.dims :refer [time-dimension? includes-dim?]]))

(defn make-tig
  "tig = Temporary ID Generator, counter for generating temporary field names used in aggregations that are later refered in post-aggregations"
  []
  (let [counter (atom -1)]
    (fn []
      (swap! counter inc))))

(defn- to-druid-agg [measure tig]
  (measure->druid measure tig))

(defn- sort-by-same? [{:keys [name sort-by]}]
  (= name (:name sort-by)))

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

(defn- dimension-spec [{:keys [name column extraction]}]
  (if extraction
    {:type "extraction"
     :outputName name
     :dimension (or column name)
     :extractionFn (if (> (count extraction) 1)
                     {:type "cascade" :extractionFns extraction}
                     (first extraction))}
    name))

(defn- add-query-type-dependant-fields [{:keys [queryType] :as dq}
                                        {:keys [dimension measures] :as q}]
  (condp = queryType
    "topN"
    (assoc dq
           :granularity "all"
           :dimension (dimension-spec dimension)
           :metric (generate-metric-field dimension measures)
           :threshold (dimension :limit (or 100)))
    "timeseries"
    (assoc dq
           :granularity (if dimension
                          {:type "period" :period (:granularity dimension)}
                          "all")
           :descending (get-in dimension [:sort-by :descending] false)
           :context {:skipEmptyBuckets true})))

(defn- with-value? [{:keys [operator value]}]
  (or (= "is" operator) (seq value)))

(defn sort-by-derived-measures
  "If we sort by a not selected metric we should send the field as an aggregation, otherwise Druid complains"
  [{:keys [sort-by] :as dim} measures]
  (if (and (:name sort-by) (not (sort-by-same? dim)) (not (includes-dim? measures sort-by)))
    [sort-by]
    []))

(defn add-druid-measures [dq measures]
  (let [tig (make-tig)]
    (->> measures
         (map #(measure->druid % tig))
         (reduce (partial merge-with concat))
         (merge dq))))

(defn add-druid-filters [dq filters]
  (let [[time-filters normal-filters] ((juxt filter remove) time-dimension? filters)]
    (-> (assoc dq :intervals (str/join "/" (-> time-filters first :interval)))
        (assoc-if-seq :filter (->> normal-filters (filter with-value?) to-druid-filter)))))

(defn add-timeout [dq]
  (assoc-in dq [:context :timeout] 30000))

(defn to-druid-query [{:keys [cube filter measures dimension] :as q}]
  (-> {:queryType (calculate-query-type q)
       :dataSource {:type "table" :name cube}}
      (add-druid-filters filter)
      (add-druid-measures (concat measures (sort-by-derived-measures dimension measures)))
      (add-query-type-dependant-fields q)
      (add-timeout)))

(defn from-druid-results [{:keys [dimension]} {:keys [queryType]} results]
  (condp = queryType
    "topN" (-> results first :result)
    "timeseries" (cond->> results
                          (:limit dimension) (take (:limit dimension))
                          true (map (fn [{:keys [result timestamp]}]
                                     (if dimension
                                       (assoc result :__time timestamp)
                                       result))))))
