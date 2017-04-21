(ns shevek.lib.collections)

(defn reverse-merge [m1 m2]
  (merge m2 m1))

(defn detect [pred coll]
  (first (filter pred coll)))

(defn assoc-if-seq [map key val]
  (cond-> map
          (seq val) (assoc key val)))

(defn sequential-or-set? [x]
  (or (sequential? x) (set? x)))

(defn wrap-coll [x]
  (if (sequential-or-set? x)
    x
    (remove nil? [x])))

(defn includes? [coll x]
  (some #{x} coll))
