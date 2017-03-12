(ns pivot.dw
  (:require [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]))

(defn- set-default-title [{:keys [name title] :or {title (str/title name)} :as record}]
  (assoc record :title title))

(defn- add-defaults [cubes]
  (map #(-> (set-default-title %)
            (assoc :dimensions (map set-default-title (:dimensions %)))
            (assoc :measures (map set-default-title (:measures %))))
       cubes))

(defn fetch-cubes []
  (dispatch :data-requested :cubes "dw/cubes" {:post-process add-defaults}))
