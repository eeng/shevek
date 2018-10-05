(ns shevek.domain.cubes
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]
            [shevek.reflow.db :as db]))

(defn- cube-names-as-keys [db cubes]
  (assoc db :cubes (zipmap (map :name cubes) cubes)))

(defevh :cubes-requested [db]
  (rpc/fetch db :cubes "schema/cubes" :handler cube-names-as-keys))

(defn fetch-cubes []
  (dispatch :cubes-requested))

(defn cubes-list []
  (vals (db/get :cubes)))
