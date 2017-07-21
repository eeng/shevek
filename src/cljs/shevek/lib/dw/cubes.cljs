(ns shevek.lib.dw.cubes
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [shevek.reflow.core :refer [dispatch]]
            [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.lib.dates :refer [parse-time]]))

(defn set-cube-defaults [{:keys [max-time] :as cube}]
  (cond-> cube
          max-time (update :max-time parse-time)))

(defn- to-map-with-name-as-key [cubes]
  (zipmap (map :name cubes) cubes))

(defevh :cubes-arrived [db cubes]
  (-> (assoc db :cubes (to-map-with-name-as-key cubes))
      (rpc/loaded :cubes)))

(defevh :cubes-requested [db]
  (rpc/call "schema.api/cubes" :handler #(dispatch :cubes-arrived %))
  (rpc/loading db :cubes))

(defn fetch-cubes []
  (dispatch :cubes-requested))

(defn cubes-list []
  (vals (db/get :cubes)))
