(ns pivot.dw
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [pivot.rpc :as rpc]
            [pivot.lib.collections :refer [reverse-merge]]
            [reflow.db :as db]))

(defn- set-default-title [{:keys [name title] :or {title (str/title name)} :as record}]
  (assoc record :title title))

(defn- set-cube-defaults [{:keys [dimensions measures] :as cube}]
  (-> cube
      set-default-title
      (assoc :dimensions (map set-default-title dimensions))
      (assoc :measures (map set-default-title measures))))

(defn- set-defaults [cubes]
  (map set-cube-defaults cubes))

(defn- to-map-with-name-as-key [cubes]
  (zipmap (map :name cubes) cubes))

; We need to update instead of assoc because if we reload the cube page the cube metadata could arrive before the cubes list.
; And it has to be a reverse-merge because the cube metadata contains more information that the cubes list.
(defevh :cubes-arrived [db cubes]
  (-> (update db :cubes reverse-merge (to-map-with-name-as-key (set-defaults cubes)))
      (rpc/loaded :cubes)))

(defevh :cubes-requested [db]
  (rpc/call "dw/cubes" :handler #(dispatch :cubes-arrived %))
  (rpc/loading db :cubes))

(defn fetch-cubes []
  (dispatch :cubes-requested))

(defn cubes-list []
  (vals (db/get :cubes)))

(defevh :cube-arrived [db {:keys [name] :as cube}]
  (assoc-in db [:cubes name] (set-cube-defaults cube)))

(defevh :cube-selected [db cube]
  (rpc/call "dw/cube" :args [cube] :handler #(dispatch :cube-arrived %))
  (dispatch :navigate :cube)
  (assoc db :query {:cube cube}))
