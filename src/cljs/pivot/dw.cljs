(ns pivot.dw
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [pivot.rpc :as rpc]))

(defn- set-default-title [{:keys [name title] :or {title (str/title name)} :as record}]
  (assoc record :title title))

(defn- add-defaults [cubes]
  (map #(-> (set-default-title %)
            (assoc :dimensions (map set-default-title (:dimensions %)))
            (assoc :measures (map set-default-title (:measures %))))
       cubes))

(defevh :cubes-arrived [db cubes]
  (-> (assoc db :cubes (add-defaults cubes))
      (rpc/loaded :cubes)))

(defevh :cubes-requested [db]
  (rpc/call "dw/cubes" :handler #(dispatch :cubes-arrived %))
  (rpc/loading db :cubes))

(defn fetch-cubes []
  (dispatch :cubes-requested))
