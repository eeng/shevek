(ns shevek.pages.cubes.helpers
  (:require [shevek.reflow.db :as db]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.rpc :as rpc]))

(defn- cube-names-as-keys [db cubes]
  (assoc db :cubes (zipmap (map :name cubes) cubes)))

(defevh :cubes/fetch [db]
  (rpc/fetch db :cubes "schema/cubes" :handler cube-names-as-keys))

(defn fetch-cubes []
  (when-not (db/get :cubes)
    (dispatch :cubes/fetch)))

(defn cubes-list []
  (vals (db/get :cubes)))

(defn get-cube [name]
  (db/get-in [:cubes name]))

(defn cube-authorized? [cube-name]
  (get-cube cube-name))

(defn cubes-fetcher [render-fn]
  (fetch-cubes)
  (fn []
    (when-let [cubes (db/get :cubes)]
      (render-fn cubes))))

(defn cube-fetcher [cube-name render-fn]
  (fetch-cubes)
  (fn []
    (when-let [cube (db/get-in [:cubes cube-name])]
      (render-fn cube))))
