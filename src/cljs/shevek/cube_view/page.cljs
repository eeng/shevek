(ns shevek.cube-view.page
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.dw :as dw]
            [shevek.lib.util :refer [every]]
            [shevek.cube-view.shared :refer [send-main-query current-cube-name]]
            [shevek.cube-view.dimensions :refer [dimensions-panel]]
            [shevek.cube-view.measures :refer [measures-panel]]
            [shevek.cube-view.filter :refer [filter-panel build-time-filter]]
            [shevek.cube-view.split :refer [split-panel]]
            [shevek.cube-view.visualization :refer [visualization-panel]]
            [shevek.cube-view.pinboard :refer [pinboard-panels]]))

(defn- init-cube-view [{:keys [cube-view] :as db} {:keys [measures] :as cube}]
  (-> cube-view
      (assoc :filter [(build-time-filter cube)]
             :split []
             :measures (->> measures (take 3) vec)
             :pinboard {:measure (first measures) :dimensions []})
      (->> (assoc db :cube-view))))

(defevh :cube-selected [db cube]
  (rpc/call "dw/cube" :args [cube] :handler #(dispatch :cube-arrived %))
  (dispatch :navigate :cube)
  (-> (assoc db :cube-view {:cube cube})
      (rpc/loading :cube-metadata)))

(defevh :cube-arrived [db {:keys [name] :as cube}]
  (let [cube (dw/set-cube-defaults cube)]
    (-> (update-in db [:cubes name] merge cube)
        (init-cube-view cube)
        (rpc/loaded :cube-metadata)
        (send-main-query))))

(defevh :max-time-arrived [db cube-name max-time]
  (update-in db [:cubes cube-name] assoc :max-time (dw/parse-max-time max-time)))

(defn fetch-max-time []
  (when (= (db/get :page) :cube)
    (let [name (current-cube-name)]
      (rpc/call "dw/max-time" :args [name] :handler #(dispatch :max-time-arrived name %)))))

(defonce timeout (every 60 fetch-max-time))

(defn page []
  [:div#cube-view
   [:div.left-column
    [dimensions-panel]
    [measures-panel]]
   [:div.center-column
    [:div
     [filter-panel]
     [split-panel]]
    [visualization-panel]]
   [:div.right-column
    [pinboard-panels]]])
