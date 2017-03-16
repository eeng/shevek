(ns pivot.lib.react)

(defn rmap [component-fn coll]
  (doall
    (for [[i x] (map-indexed vector coll)]
      ^{:key i} [component-fn x])))

(defn with-react-keys [coll]
  (map #(with-meta %2 {:key %1}) (range) coll))
