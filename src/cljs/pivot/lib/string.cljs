(ns pivot.lib.string
  (:require [goog.string :as gstring]))

(defn format
  "Formats a string using goog.string.format."
  [fmt & args]
  (apply gstring/format fmt args))
