(ns shevek.lib.mongodb
  (:require [clj-time.core :refer [now]]
            [monger.joda-time]) ; Necessary for automatic joda time objects persistance in monger
  (:import [org.bson.types ObjectId]))

(defn oid [str]
  (ObjectId. str))

(defn timestamp [{:keys [created-at] :as record}]
  (let [updated-at (now)]
    (cond-> (assoc record :updated-at updated-at)
            (not created-at) (assoc :created-at updated-at))))
