(ns shevek.lib.collections)

(defn reverse-merge [m1 m2]
  (merge m2 m1))

(defn detect [pred coll]
  (first (filter pred coll)))
