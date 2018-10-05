(ns shevek.domain.pivot-table
  (:require [shevek.lib.collections :refer [detect]]
            [shevek.domain.dw :refer [dimension-value measure-value format-measure format-dimension]]
            [shevek.lib.dw.dims :refer [partition-splits]]
            [shevek.i18n :refer [t]]))

(defrecord SplitsCell [dimensions in-columns col-span])
(defrecord MeasureCell [measure splits top-left-corner])
(defrecord DimensionValueCell [value text col-span depth in-columns dimension])
(defrecord MeasureValueCell [value text proportion])
(defrecord EmptyCell [])

; Constructors

(defn splits-cell [dimensions & {:keys [in-columns col-span] :or {in-columns false col-span 1}}]
  (SplitsCell. dimensions in-columns col-span))

(defn measure-cell [measure & {:keys [splits top-left-corner]}]
  (MeasureCell. measure (or splits []) (or top-left-corner false)))

(defn measure-value-cell [measure result & {:keys [max-value] :or {max-value 0}}]
  (let [value (measure-value measure result)
        rate (if (zero? max-value) 0 (/ value max-value))
        proportion (* (Math/abs rate) 100)
        proportion (if (> proportion 100) 0 proportion)] ; if is the grand total row we don't show this data
    (MeasureValueCell. value
                       (or (format-measure measure result) "")
                       proportion)))

(defn dimension-value-cell
  [dim result & {:keys [col-span depth in-columns] :or {col-span 1 depth 0 in-columns false}}]
  (DimensionValueCell. (dimension-value dim result)
                       (format-dimension dim result)
                       col-span
                       depth
                       in-columns
                       dim)) ; Needed for the slice used in the rows popup

(defn grand-total-cell [& {:keys [col-span in-columns] :or {col-span 1 in-columns false}}]
  (DimensionValueCell. nil
                       (t :viewer/grand-total)
                       col-span
                       0
                       in-columns
                       nil))

(def empty-cell ->EmptyCell)

; Generation logic

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

(defn- top-header-rows [{:keys [col-splits measures splits results] :as viz}]
  (when (seq col-splits)
    (let [measure-cell (if (multiple-measures-layout? viz)
                         (empty-cell)
                         (measure-cell (first measures) :splits splits :top-left-corner true))
          col-span (calculate-col-span (first results) measures)
          col-split-cell (splits-cell col-splits :in-columns true :col-span col-span)]
      [[measure-cell col-split-cell]])))

(defn- col-dim-value-cell [[col-split & other-col-splits] {:keys [grand-total?] :as result} measures]
  (let [col-span (calculate-col-span result measures)]
    (if grand-total?
      (if (seq other-col-splits)
        (empty-cell)
        (grand-total-cell :col-span col-span :in-columns true))
      (dimension-value-cell col-split result :col-span col-span :in-columns true))))

(defn- measures-cells [measures results splits]
  (->> (map #(measure-cell % :splits splits) measures)
       (repeat (count results))
       (apply concat)))

(defn- bottom-header-rows
  [[col-split & other-col-splits :as col-splits] results rows {:keys [row-splits measures splits] :as viz}]
  (if col-split
    (let [first-cell (if (and (empty? other-col-splits) (seq row-splits) (not (multiple-measures-layout? viz)))
                       (splits-cell row-splits)
                       (empty-cell))
          this-row (cons first-cell (map #(col-dim-value-cell col-splits % measures) results))
          next-results (mapcat child-cols-and-self results)]
      (bottom-header-rows other-col-splits next-results (conj rows this-row) viz))
    (if (multiple-measures-layout? viz)
      (let [first-cell (if (empty? row-splits) (empty-cell) (splits-cell row-splits))]
        (conj rows (cons first-cell (measures-cells measures results splits))))
      rows)))

(defn- measure-values-cells [result {:keys [results col-splits measures max-values]}]
  (for [result (flatten-result result (first results) col-splits)
        measure measures]
    (measure-value-cell measure result :max-value (-> measure :name keyword max-values))))

(defn- result-row [{:keys [grand-total?] :as result} row-split viz slice-so-far]
  (let [first-cell (if grand-total?
                     (grand-total-cell)
                     (dimension-value-cell row-split result :depth (count slice-so-far)))]
    {:cells (cons first-cell (measure-values-cells result viz))
     :slice (conj slice-so-far first-cell)
     :grand-total? grand-total?}))

(defn- results-rows
  [{:keys [child-rows] :as result} viz [row-split & row-splits] slice-so-far]
  (when result
    (let [this-row (result-row result row-split viz slice-so-far)
          next-rows (mapcat #(results-rows % viz row-splits (:slice this-row)) child-rows)]
      (cons this-row next-rows))))

(defn- calculate-max-values [measures [_ & results]]
  (reduce (fn [max-values measure-name]
            (assoc max-values measure-name (->> results (map measure-name) (map Math/abs) (apply max))))
          {}
          (map (comp keyword :name) measures)))

(defn generate [{:keys [splits results measures] :as viz}]
  (let [grand-total (assoc (first results) :grand-total? true)
        results (cons grand-total (rest results))
        [row-splits col-splits] (partition-splits splits)
        max-values (calculate-max-values measures results)
        viz (assoc viz :row-splits row-splits :col-splits col-splits :max-values max-values)]
    {:head (concat (top-header-rows viz)
                   (bottom-header-rows col-splits (child-cols-and-self grand-total) [] viz))
     :body (mapcat #(results-rows % viz row-splits []) results)}))
