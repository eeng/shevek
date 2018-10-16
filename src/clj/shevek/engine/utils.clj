(ns shevek.engine.utils
  (:require [clj-time.core :as t]))

(def defaultLimit 100)

(defn time-zone [q]
  (or (:time-zone q) (str (t/default-time-zone))))
