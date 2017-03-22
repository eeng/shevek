(ns pivot.lib.react)

(defn rmap [component-fn coll]
  (doall
    (for [[i x] (map-indexed vector coll)]
      ^{:key i} [component-fn x])))

(defn with-react-keys [coll]
  (doall (map #(with-meta %2 {:key %1}) (range) coll)))

(defn without-propagation [f & args]
  (fn [e]
    (apply f args)
    (.preventDefault e)
    (.stopPropagation e)))
