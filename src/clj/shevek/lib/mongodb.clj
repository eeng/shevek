(ns shevek.lib.mongodb
  (:import [org.bson.types ObjectId]))

(defn oid [str]
  (ObjectId. str))
