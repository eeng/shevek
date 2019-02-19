(ns shevek.lib.react
  (:require [reagent.core :as r]))

(defn rmap [component-fn key-fn coll]
  (for [x coll]
    ^{:key (key-fn x)} [component-fn x]))

; TODO DEPRECATED replace with [:<>]
(defn with-react-keys [coll]
  (doall (map #(with-meta %2 {:key %1}) (range) coll)))

(defn without-propagation [f & args]
  (fn [e]
    (apply f args)
    (.preventDefault e)
    (.stopPropagation e)))

(defonce hot-reloading (r/atom false))

(defn with-reload-mark [remount]
  (reset! hot-reloading true)
  (remount)
  (reset! hot-reloading false))

(defn hot-reloading? []
  @hot-reloading)
