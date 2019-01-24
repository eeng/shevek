(ns shevek.lib.collections
  (:require [com.rpl.specter :refer [transform setval MAP-KEYS NONE ALL keypath]]))

(defn reverse-merge [m1 m2]
  (merge m2 m1))

(defn detect [pred coll]
  (first (filter pred coll)))

(defn find-by [key value coll]
  (detect #(= (get % key) value) coll))

(defn assoc-if
  "assoc key/value pairs to the map only on non-nil values

   (assoc-if {} :a 1)
   => {:a 1}

   (assoc-if {} :a 1 :b nil)
   => {:a 1}"
  ([m k v]
   (if (not (nil? v)) (assoc m k v) m))
  ([m k v & more]
   (apply assoc-if (assoc-if m k v) more)))

(defn assoc-if-seq [m & entries]
  (reduce (fn [m [key val]]
            (cond-> m
                    (seq val) (assoc key val)))
          m
          (partition 2 entries)))

(defn assoc-nil
  "only assoc if the value in the original map is nil

   (assoc-nil {:a 1} :b 2)
   => {:a 1 :b 2}

   (assoc-nil {:a 1} :a 2 :b 2)
   => {:a 1 :b 2}"
  ([m k v]
   (if (not (nil? (get m k))) m (assoc m k v)))
  ([m k v & more]
   (apply assoc-nil (assoc-nil m k v) more)))

(defn sequential-or-set? [x]
  (or (sequential? x) (set? x)))

(defn wrap-coll [x]
  (if (sequential-or-set? x)
    x
    (remove nil? [x])))

(defn includes? [coll x]
  (some #(= % x) coll))

(defn index-of [coll value]
  (some (fn [[idx item]] (if (= value item) idx))
        (map-indexed vector coll)))

(defn stringify-keys [m]
  (transform MAP-KEYS name m))

(defn distinct-by
  "Returns a lazy sequence of the elements of coll, removing any elements that
  return duplicate values when passed to a function f."
  ([f]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result x]
          (let [fx (f x)]
            (if (contains? @seen fx)
              result
              (do (vswap! seen conj fx)
                  (rf result x)))))))))
  ([f coll]
   (let [step (fn step [xs seen]
                (lazy-seq
                 ((fn [[x :as xs] seen]
                    (when-let [s (seq xs)]
                      (let [fx (f x)]
                        (if (contains? seen fx)
                          (recur (rest s) seen)
                          (cons x (step (rest s) (conj seen fx)))))))
                  xs seen)))]
     (step coll #{}))))
