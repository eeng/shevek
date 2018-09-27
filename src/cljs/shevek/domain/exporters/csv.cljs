(ns shevek.domain.exporters.csv
  (:require [testdouble.cljs.csv :refer [write-csv]]
            [cuerdas.core :as str]
            [shevek.domain.pivot-table :refer [multiple-measures-layout? flatten-result child-cols-and-self]]
            [shevek.lib.dw.dims :refer [partition-splits]]
            [shevek.viewer.shared :refer [format-dimension measure-value]]))

(defn- write-excel-csv [data]
  (write-csv data :newline :cr+lf :quote? true))

(defn splits-cell [splits]
  (str/join ", " (map :title splits)))

(defn- top-header-rows [{:keys [col-splits measures] :as viz}]
  (when (seq col-splits)
    (let [measure-cell (if (multiple-measures-layout? viz) "" (-> measures first :title))
          col-split-cell (splits-cell col-splits)]
      [[measure-cell col-split-cell]])))

(defn- calculate-col-span [{:keys [child-cols] :as result} measures]
  (if (seq child-cols)
    (->> (child-cols-and-self result)
         (map #(calculate-col-span % measures))
         (reduce +))
    (count measures)))

(defn- col-dims-values-cells [dim result measures]
  (let [col-span (calculate-col-span result measures)
        empty-cells (repeat (dec col-span) "")]
    (into [(format-dimension dim result)] empty-cells)))

(defn- measures-cells [measures results]
  (->> (map :title measures)
       (repeat (count results))
       (apply concat)))

(defn- bottom-header-rows
  [[col-split & col-splits] results rows {:keys [row-splits measures] :as viz}]
  (if col-split
    (let [row-splits-cell (if (and (empty? col-splits) (not (multiple-measures-layout? viz)))
                            (splits-cell row-splits)
                            "")
          this-row (into [row-splits-cell] (mapcat #(col-dims-values-cells col-split % measures) results))
          next-results (mapcat child-cols-and-self results)]
      (bottom-header-rows col-splits next-results (conj rows this-row) viz))
    (if (multiple-measures-layout? viz)
      (conj rows (into [(splits-cell row-splits)] (measures-cells measures results)))
      rows)))

(defn- measure-values-cells [result {:keys [results col-splits measures]}]
  (for [result (flatten-result result (first results) col-splits)
        measure measures]
    (measure-value measure result)))

(defn- results-rows
  [{:keys [child-rows] :as result} viz [row-split & row-splits] depth]
  (when result
    (let [indentation (apply str (repeat (* depth 5) " "))
          this-row (into [(str indentation (format-dimension row-split result))]
                         (measure-values-cells result viz))
          next-rows (mapcat #(results-rows % viz row-splits (inc depth)) child-rows)]
      (into [this-row] next-rows))))

(defn generate [{:keys [splits results] :as viz}]
  (let [[row-splits col-splits] (partition-splits splits)
        viz (assoc viz :row-splits row-splits :col-splits col-splits)]
    (write-excel-csv
     (concat
      (top-header-rows viz)
      (bottom-header-rows col-splits (child-cols-and-self (first results)) [] viz)
      (mapcat #(results-rows % viz row-splits 0) results)))))
