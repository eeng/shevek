(ns shevek.lib.dw.cubes
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]
            [shevek.lib.dates :refer [parse-time]]))

(defn set-cube-defaults [{:keys [max-time] :as cube}]
  (cond-> cube
          max-time (update :max-time parse-time)))

(defn- cube-names-as-keys [db cubes]
  (assoc db :cubes (zipmap (map :name cubes) cubes)))

(defevh :cubes-requested [db]
  (rpc/fetch db :cubes "schema/cubes" :handler cube-names-as-keys))

(defn fetch-cubes []
  (dispatch :cubes-requested))

(defn cubes-list []
  (vals (db/get :cubes)))
