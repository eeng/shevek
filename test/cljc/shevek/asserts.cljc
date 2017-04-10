(ns shevek.asserts
  (:require [shevek.lib.collections :refer [wrap-coll]]))

(defn submap? [sm m]
  {:pre [(map? sm)]}
  (and (map? m)
       (= sm (select-keys m (keys sm)))))

(defn submaps? [submaps maps]
  (and (every? (fn [[sm m]] (submap? sm m))
               (map vector submaps maps))
       (= (count submaps) (count maps))))

(defn without? [k m]
  (not (contains? m k)))

(defn error-on?
  ([field record]
   (seq (get-in (:errors record) (wrap-coll field))))
  ([field msg record]
   (some #(.contains % msg) (get-in (:errors record) (wrap-coll field)))))

(def no-error-on? (complement error-on?))
