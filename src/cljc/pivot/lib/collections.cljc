(ns pivot.lib.collections)

(defn reverse-merge [m1 m2]
  (merge m2 m1))

(defn detect [pred coll]
  (first (filter pred coll)))

(defn replace-matching
  "Replaces in the collection the value that matches the predicate with the new value"
  [pred new-value coll]
  (map #(if (pred %) new-value %) coll))
