(ns shevek.engine.druid.planner.common
  (:require [clj-time.core :as t]
            [shevek.domain.dimension :refer [find-dimension numeric-dim? time-dimension? includes-dim?]]
            [shevek.querying.expression :refer [measure->druid]]
            [shevek.lib.collections :refer [assoc-if-seq]]
            [clojure.string :as str]))

(def defaultLimit 100)

(defn time-zone [q]
  (or (:time-zone q) (str (t/default-time-zone))))

(defn- list-filtered-values [{:keys [multi-value name]} {:keys [filters]}]
  (when multi-value
    (let [filter (find-dimension name filters)]
      (when (-> filter :operator (= "include"))
        (:value filter)))))

(defn- add-time-zome-to-extraction-fn [{:keys [type] :as extraction-fn} q]
  (cond-> extraction-fn
    (= type "timeFormat") (assoc :timeZone (time-zone q))))

(defn- dimension-and-extraction [{:keys [name column extraction]} q]
  (let [extraction (map #(add-time-zome-to-extraction-fn % q) extraction)]
    (cond-> {:dimension (or column name)}
      (seq extraction)
      (assoc :extractionFn (if (> (count extraction) 1)
                             {:type "cascade" :extractionFns extraction}
                             (first extraction))))))

(defn dimension-spec [{:keys [name extraction] :as dim} q]
  (let [lfv (list-filtered-values dim q)]
    (cond
      extraction (assoc (dimension-and-extraction dim q) :type "extraction" :outputName name)
      lfv {:type "listFiltered" :delegate name :values lfv}
      :else name)))

(defn sort-by-same? [{:keys [name sort-by]}]
  (= name (:name sort-by)))

(defn measure? [dim-or-measure]
  (contains? dim-or-measure :expression))

(defn sort-by-other-dimension? [{:keys [sort-by] :as dim}]
  (and sort-by
       (not (sort-by-same? dim))
       (not (measure? sort-by))))

(defn dimension-order [dim]
  (if (numeric-dim? dim) "numeric" "lexicographic"))

(defn- to-druid-filter [[{:keys [operator value] :as dim} :as filters] q]
  (let [base-filter (dimension-and-extraction dim q)]
    (condp = (count filters)
      0 nil
      1 (condp = (keyword operator)
          :is (assoc base-filter :type "selector" :value value)
          :include (assoc base-filter :type "in" :values value)
          :exclude {:type "not" :field (assoc base-filter :type "in" :values value)}
          :search (assoc base-filter :type "search" :query {:type "insensitive_contains" :value value}))
      {:type "and" :fields (map #(to-druid-filter [%] q) filters)})))

(defn- with-value? [{:keys [operator value]}]
  (or (= "is" operator) (seq value)))

(defn add-druid-filters [dq q]
  (let [[time-filters normal-filters] ((juxt filter remove) time-dimension? (:filters q))]
    (-> (assoc dq :intervals (str/join "/" (-> time-filters first :interval)))
        (assoc-if-seq :filter (to-druid-filter (->> normal-filters (filter with-value?)) q)))))

(defn sort-by-derived-measures
  "If we sort by a not selected metric we should send the field as an aggregation, otherwise Druid complains"
  [{:keys [sort-by] :as dim} measures]
  (if (and (:name sort-by)
           (measure? sort-by)
           (not (includes-dim? measures sort-by)))
    [sort-by]
    []))

(defn make-tig
  "tig = Temporary ID Generator, counter for generating temporary field names used in aggregations that are later refered in post-aggregations"
  []
  (let [counter (atom -1)]
    (fn []
      (swap! counter inc))))

(defn add-druid-measures [dq {:keys [dimension measures]}]
  (let [measures (concat measures (sort-by-derived-measures dimension measures))
        tig (make-tig)]
    (->> measures
         (map #(measure->druid % tig))
         (reduce (partial merge-with concat))
         (merge dq))))

(defn add-timeout [dq]
  (assoc-in dq [:context :timeout] 30000))

(defn add-common-fields [dq q]
  (-> dq
      (add-druid-filters q)
      (add-druid-measures q)
      (add-timeout)))
