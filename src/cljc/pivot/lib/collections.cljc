(ns pivot.lib.collections)

(defn detect [pred coll]
  (first (filter pred coll)))
