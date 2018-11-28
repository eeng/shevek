(ns shevek.engine.druid-native.solver.common
  (:require [shevek.domain.dimension :refer [find-dimension numeric-dim? time-dimension? includes-dim? measure? sort-by-other-dimension?]]
            [shevek.engine.druid-native.solver.expression :refer [measure->druid]]
            [shevek.engine.utils :refer [time-zone]]
            [shevek.lib.collections :refer [assoc-if-seq distinct-by]]
            [clojure.string :as str]))

(defn- list-filtered-values [{:keys [multi-value name]} {:keys [filters]}]
  (when multi-value
    (let [filter (find-dimension name filters)]
      (when (-> filter :operator (= "include"))
        (:value filter)))))

(defn- add-time-zome-to-extraction-fn [{:keys [type] :as extraction-fn} q]
  (cond-> extraction-fn
    (= type "timeFormat") (assoc :timeZone (time-zone q))))

(defn virtual-column-name [{:keys [name]}]
  (str name ":v"))

(defn- dimension-column-name [{:keys [name column expression] :as dim}]
  (if expression
    (virtual-column-name dim)
    (or column name)))

(defn- dimension-and-extraction [{:keys [extraction] :as dim} q]
  (let [extraction (map #(add-time-zome-to-extraction-fn % q) extraction)]
    (cond-> {:dimension (dimension-column-name dim)}
      (seq extraction)
      (assoc :extractionFn (if (> (count extraction) 1)
                             {:type "cascade" :extractionFns extraction}
                             (first extraction))))))

(defn dimension-spec [{:keys [name extraction expression type] :as dim} q]
  (let [lfv (list-filtered-values dim q)]
    (cond
      extraction (assoc (dimension-and-extraction dim q) :type "extraction" :outputName name)
      lfv {:type "listFiltered" :delegate name :values lfv}
      expression {:type "default" :dimension (virtual-column-name dim) :outputName name :outputType type}
      :else name)))

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
  [{:keys [sort-by]} measures]
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

(defn- generate-virtual-column [{:keys [expression type] :as dim}]
  (when expression
    {:type "expression"
     :name (virtual-column-name dim)
     :expression expression
     :outputType type}))

(defn generate-virtual-columns [dimensions]
  (->> dimensions
       (map generate-virtual-column)
       (remove nil?)
       (distinct-by :name)))

; The sort-by-dim is a hack needed because measures at this time have our own expression language
(defn- add-virtual-columns [dq {:keys [dimension filters]}]
  (let [sort-by-dim (when-not (measure? (:sort-by dimension)) (:sort-by dimension))]
    (assoc dq :virtualColumns (generate-virtual-columns (into [dimension sort-by-dim] filters)))))

(defn add-common-fields [dq q]
  (-> dq
      (add-virtual-columns q)
      (add-druid-filters q)
      (add-druid-measures q)
      (add-timeout)))
