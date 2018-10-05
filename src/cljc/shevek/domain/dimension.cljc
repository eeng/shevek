(ns shevek.domain.dimension
  (:require [shevek.lib.collections :refer [detect]]
            [shevek.schemas.cube :refer [Dimension]]
            [schema-tools.core :as st]
            [clojure.string :refer [lower-case]]
            [com.rpl.specter :refer [transform ALL]]))

(defn dim= [dim1 dim2]
  (= (:name dim1) (:name dim2)))

(defn includes-dim? [coll dim]
  (some #(dim= % dim) coll))

(defn time-dimension? [{:keys [name interval period granularity]}]
  (or (= name "__time") interval period granularity))

(defn time-dimension [dimensions]
  (some #(when (time-dimension? %) %) dimensions))

(defn find-dimension [name dimensions]
  (detect #(= (:name %) name) dimensions))

(defn clean-dim [dim]
  (st/select-schema dim Dimension))

(defn add-dimension [coll dim]
  (let [coll (or coll [])]
    (if (includes-dim? coll dim)
      coll
      (conj coll dim))))

(defn remove-dimension [coll dim]
  (vec (remove #(dim= dim %) coll)))

(defn replace-dimension
  ([coll dim] (replace-dimension coll dim dim))
  ([coll old-dim new-dim]
   (transform [ALL] #(condp dim= %
                       old-dim new-dim
                       new-dim old-dim
                       %)
              coll)))

(defn add-or-replace [coll dim]
  (if (includes-dim? coll dim)
    (replace-dimension coll dim)
    (conj coll dim)))

(defn merge-dimensions [current-dims other-dims]
  (reduce add-or-replace current-dims other-dims))

(defn numeric-dim? [{:keys [type]}]
  (some #{"LONG" "FLOAT"} [type]))

(defn row-split? [{:keys [on] :or {on "rows"}}]
  (= on "rows"))

(def col-split? (comp not row-split?))

(defn partition-splits [splits]
  ((juxt filter remove) row-split? splits))
