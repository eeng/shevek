(ns shevek.domain.exporters.csv
  (:require [testdouble.cljs.csv :refer [write-csv]]
            [cuerdas.core :as str]
            [shevek.domain.pivot-table :as pivot-table :refer [SplitsCell MeasureCell DimensionValueCell MeasureValueCell EmptyCell]]
            [shevek.lib.collections :refer [wrap-coll]]))

(defn- write-excel-csv [data]
  (write-csv data :newline :cr+lf :quote? true))

(defprotocol CsvCell
  (as-text [this]))

(extend-protocol CsvCell
  EmptyCell
  (as-text [cell] "")

  SplitsCell
  (as-text [cell] (->> cell :dimensions (map :title) (str/join ", ")))

  MeasureCell
  (as-text [cell] (-> cell :measure :title))

  MeasureValueCell
  (as-text [cell] (:value cell))

  DimensionValueCell
  (as-text [{:keys [text depth col-span]}]
    (let [indentation (apply str (repeat (* depth 5) " "))
          fill-cells (repeat (dec col-span) "")]
      (cons (str indentation text) fill-cells))))

(defn- texts [cell]
  (wrap-coll (or (as-text cell) "")))

(defn generate [viz]
  (let [{:keys [head body]} (pivot-table/generate viz)]
    (write-excel-csv
     (concat
      (for [row head]
        (mapcat texts row))
      (for [{:keys [cells]} body]
        (mapcat texts cells))))))
