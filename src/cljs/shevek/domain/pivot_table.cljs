(ns shevek.domain.pivot-table
  (:require [shevek.lib.collections :refer [detect]]
            [shevek.viewer.shared :refer [dimension-value measure-value format-measure format-dimension]]
            [shevek.lib.dw.dims :refer [partition-splits]]))

(defrecord SplitsCell [dimensions])
(defrecord MeasureCell [measure])
(defrecord DimensionValueCell [value text col-span])
(defrecord MeasureValueCell [value text])
(defrecord EmptyCell [])
(defrecord GrandTotalCell [])

(def empty-cell ->EmptyCell)
(def grand-total-cell ->GrandTotalCell)
(def measure-cell ->MeasureCell)
(def splits-cell ->SplitsCell)

(defn measure-value-cell [measure result]
  (MeasureValueCell. (measure-value measure result) (format-measure measure result)))

(defn dimension-value-cell [dim result & {:keys [col-span] :or {col-span 1}}]
  (DimensionValueCell. (dimension-value dim result) (format-dimension dim result) col-span))

(defn multiple-measures-layout? [{:keys [measures col-splits]}]
  (or (> (count measures) 1) (empty? col-splits)))

(defn flatten-result [result grand-total [col-split & col-splits]]
  (let [corresponding (fn [result coll]
                        (or (detect #(= (dimension-value col-split %) (dimension-value col-split result)) coll)
                            {(keyword (:name col-split)) (dimension-value col-split result)}))
        completed-children (map #(corresponding % (:child-cols result)) (:child-cols grand-total))]
    (concat (mapcat #(flatten-result % (corresponding % (:child-cols grand-total)) col-splits)
                    completed-children)
            [(dissoc result :child-cols :child-rows)])))

(defn child-cols-and-self [{:keys [child-cols] :as result}]
  (concat child-cols [(dissoc result :child-cols)]))

(defn- calculate-col-span [{:keys [child-cols] :as result} measures]
  (if (seq child-cols)
    (->> (child-cols-and-self result)
         (map #(calculate-col-span % measures))
         (reduce +))
    (count measures)))

(defn- top-header-rows [{:keys [col-splits measures] :as viz}]
  (when (seq col-splits)
    (let [measure-cell (if (multiple-measures-layout? viz)
                         (empty-cell)
                         (measure-cell (first measures)))
          col-split-cell (splits-cell col-splits)]
      [[measure-cell col-split-cell]])))

(defn- col-dim-value-cell [dim result measures]
  (let [col-span (calculate-col-span result measures)]
    (dimension-value-cell dim result :col-span col-span)))

(defn- measures-cells [measures results]
  (->> (map measure-cell measures)
       (repeat (count results))
       (apply concat)))

(defn- bottom-header-rows
  [[col-split & col-splits] results rows {:keys [row-splits measures] :as viz}]
  (if col-split
    (let [first-cell (if (and (empty? col-splits) (seq row-splits) (not (multiple-measures-layout? viz)))
                       (splits-cell row-splits)
                       (empty-cell))
          this-row (into [first-cell] (map #(col-dim-value-cell col-split % measures) results))
          next-results (mapcat child-cols-and-self results)]
      (bottom-header-rows col-splits next-results (conj rows this-row) viz))
    (if (multiple-measures-layout? viz)
      (let [first-cell (if (empty? row-splits) (empty-cell) (splits-cell row-splits))]
        (conj rows (into [first-cell] (measures-cells measures results))))
      rows)))

(defn- measure-values-cells [result {:keys [results col-splits measures]}]
  (for [result (flatten-result result (first results) col-splits)
        measure measures]
    (measure-value-cell measure result)))

(defn- result-row [{:keys [grand-total?] :as result} row-split viz slice]
  (let [first-cell (if grand-total?
                     (grand-total-cell)
                     (dimension-value-cell row-split result))]
    {:cells (into [first-cell] (measure-values-cells result viz))
     :slice slice
     :depth (dec (count slice))}))

(defn- results-rows
  [{:keys [child-rows] :as result} viz [row-split & row-splits] slice-so-far]
  (when result
    (let [slice (conj slice-so-far [row-split (dimension-value row-split result)])
          this-row (result-row result row-split viz slice)
          next-rows (mapcat #(results-rows % viz row-splits slice) child-rows)]
      (into [this-row] next-rows))))

(defn generate [{:keys [splits results] :as viz}]
  (let [[row-splits col-splits] (partition-splits splits)
        viz (assoc viz :row-splits row-splits :col-splits col-splits)
        grand-total (assoc (first results) :grand-total? true)
        results (into [grand-total] (rest results))]
    {:head (concat (top-header-rows viz)
                   (bottom-header-rows col-splits (child-cols-and-self grand-total) [] viz))
     :body (mapcat #(results-rows % viz row-splits []) results)}))
