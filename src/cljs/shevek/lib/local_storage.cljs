(ns shevek.lib.local-storage
  (:require [cljs.reader :refer [read-string]]))

(defn set-item!
  "Set `key' in browser's localStorage to `val`."
  [key val]
  (.setItem (.-localStorage js/window) key val))

(defn get-item
  "Returns value of `key' from browser's localStorage."
  [key]
  (.getItem (.-localStorage js/window) key))

(defn store! [key value]
  (->> value pr-str (set-item! key)))

(defn retrieve [key]
  (-> key get-item str read-string))
