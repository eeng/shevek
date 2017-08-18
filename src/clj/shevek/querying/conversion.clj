(ns shevek.querying.conversion
  (:require [clojure.string :as str]
            [clj-time.core :as t]
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

(defn- time-zone [{:keys [default-time-zone]}]
  (or default-time-zone (str (t/default-time-zone))))

(defn- add-time-zome-to-extraction-fn [{:keys [type] :as extraction-fn} schema]
  (cond-> extraction-fn
          (= type "timeFormat") (assoc :timeZone (time-zone schema))))

(defn- dimension-and-extraction [{:keys [name column extraction]} schema]
  (let [extraction (map #(add-time-zome-to-extraction-fn % schema) extraction)]
    (cond-> {:dimension (or column name)}
            (seq extraction)
            (assoc :extractionFn (if (> (count extraction) 1)
                                   {:type "cascade" :extractionFns extraction}
                                   (first extraction))))))

(defn- to-druid-filter [[{:keys [operator value] :as dim} :as filters] schema]
  (let [base-filter (dimension-and-extraction dim schema)]
    (condp = (count filters)
      0 nil
      1 (condp = (keyword operator)
          :is (assoc base-filter :type "selector" :value value)
          :include (assoc base-filter :type "in" :values value)
          :exclude {:type "not" :field (assoc base-filter :type "in" :values value)}
          :search (assoc base-filter :type "search" :query {:type "insensitive_contains" :value value}))
      {:type "and" :fields (map #(to-druid-filter [%] schema) filters)})))

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

(defn- dimension-spec [{:keys [name extraction] :as dim} schema]
  (if extraction
    (assoc (dimension-and-extraction dim schema) :type "extraction" :outputName name)
    name))

(defn- add-query-type-dependant-fields [{:keys [queryType] :as dq}
                                        {:keys [dimension measures] :as q}
                                        schema]
  (condp = queryType
    "topN"
    (assoc dq
           :granularity "all"
           :dimension (dimension-spec dimension schema)
           :metric (generate-metric-field dimension measures)
           :threshold (dimension :limit (or 100)))
    "timeseries"
    (assoc dq
           :granularity (if dimension
                          {:type "period" :period (:granularity dimension) :timeZone (time-zone schema)}
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

(defn add-druid-filters [dq filters schema]
  (let [[time-filters normal-filters] ((juxt filter remove) time-dimension? filters)]
    (-> (assoc dq :intervals (str/join "/" (-> time-filters first :interval)))
        (assoc-if-seq :filter (to-druid-filter (->> normal-filters (filter with-value?)) schema)))))

(defn add-timeout [dq]
  (assoc-in dq [:context :timeout] 30000))

(defn to-druid-query [{:keys [cube filter measures dimension] :as q} schema]
  (-> {:queryType (calculate-query-type q)
       :dataSource {:type "table" :name cube}}
      (add-druid-filters filter schema)
      (add-druid-measures (concat measures (sort-by-derived-measures dimension measures)))
      (add-query-type-dependant-fields q schema)
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
