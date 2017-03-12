(ns pivot.react)

(defn rmap [component-fn coll]
  (doall
    (for [[i x] (map-indexed vector coll)]
      ^{:key i} [component-fn x])))
