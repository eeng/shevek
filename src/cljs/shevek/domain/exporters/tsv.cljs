(ns shevek.domain.exporters.tsv
  (:require [testdouble.cljs.csv :refer [write-csv]]
            [clojure.string :as str]
            [shevek.lib.dw.dims :refer [partition-splits]]))

(defn- write-excel-tsv [headers content]
  (let [data (concat [headers] content)]
    (write-csv data :newline :cr+lf)))

(defn- result-values [result dims-or-measures]
  (for [{:keys [name]} dims-or-measures]
    (get result (keyword name))))

(defn- build-row [{:keys [row-splits measures results]} result]
  (let [row-headers (result-values result row-splits)
        row-headers (if (and (seq row-splits) (empty? (remove str/blank? row-headers)))
                      (into ["Total"] (rest row-headers))
                      row-headers)]
    (concat row-headers (result-values result measures))))

(defn- build-rows [{:keys [measures] :as viz} {:keys [child-rows] :as result}]
  (when result
    (let [dim-values (apply dissoc result (conj (map (comp keyword :name) measures) :child-rows))
          child-rows (map #(merge % dim-values) child-rows)]
      (into
       [(build-row viz result)]
       (mapcat (partial build-rows viz) child-rows)))))

(defn- build-headers [{:keys [row-splits measures results] :as viz}]
  (concat (map :title row-splits)
          (map :title measures)))

(defn generate [{:keys [splits results] :as viz}]
  (let [[row-splits col-splits] (partition-splits splits)
        viz (assoc viz :row-splits row-splits :col-splits col-splits)]
    (write-excel-tsv
     (build-headers viz)
     (mapcat (partial build-rows viz) results))))
