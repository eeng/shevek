(ns shevek.lib.react)

(defn rmap [component-fn coll key-fn]
  (for [x coll]
    ^{:key (key-fn x)} [component-fn x]))

(defn with-react-keys [coll]
  (doall (map #(with-meta %2 {:key %1}) (range) coll)))

(defn without-propagation [f & args]
  (fn [e]
    (apply f args)
    (.preventDefault e)
    (.stopPropagation e)))
