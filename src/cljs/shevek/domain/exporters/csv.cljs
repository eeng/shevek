(ns shevek.domain.exporters.csv
  (:require [testdouble.cljs.csv :refer [write-csv]]
            [cuerdas.core :as str]
            [shevek.domain.pivot-table :as pivot-table :refer [SplitsCell MeasureCell DimensionValueCell MeasureValueCell EmptyCell GrandTotalCell]]
            [shevek.lib.collections :refer [wrap-coll]]
            [shevek.i18n :refer [t]]))

(defn- write-excel-csv [data]
  (write-csv data :newline :cr+lf :quote? true))

(defprotocol CsvCell
  (text [this]))

(extend-protocol CsvCell
  EmptyCell
  (text [cell] "")

  SplitsCell
  (text [cell] (->> cell :dimensions (map :title) (str/join ", ")))

  MeasureCell
  (text [cell] (-> cell :measure :title))

  GrandTotalCell
  (text [cell] (t :viewer/grand-total))

  MeasureValueCell
  (text [cell] (:text cell))

  DimensionValueCell
  (text [{:keys [text depth col-span]}]
    (let [indentation (apply str (repeat (* depth 5) " "))
          fill-cells (repeat (dec col-span) "")]
      (cons (str indentation text) fill-cells))))

(defn- texts [cell]
  (wrap-coll (text cell)))

(defn generate [viz]
  (let [{:keys [head body] :as pt} (pivot-table/generate viz)]
    (write-excel-csv
     (concat
      (for [row head]
        (mapcat texts row))
      (for [{:keys [cells]} body]
        (mapcat texts cells))))))
