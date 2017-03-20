(ns pivot.lib.collections)

(defn reverse-merge [m1 m2]
  (merge m2 m1))

(defn detect [pred coll]
  (first (filter pred coll)))

(defn replace-when
  "Replaces in the collection the value that matches the predicate with the result of evaluating the update-fn with that value."
  [pred update-fn coll]
  (map #(if (pred %) (update-fn %) %) coll))
