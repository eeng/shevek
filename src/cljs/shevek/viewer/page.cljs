(ns shevek.viewer.page
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.dw :as dw]
            [shevek.lib.util :refer [every]]
            [shevek.viewer.shared :refer [send-main-query current-cube-name]]
            [shevek.viewer.dimensions :refer [dimensions-panel]]
            [shevek.viewer.measures :refer [measures-panel]]
            [shevek.viewer.filter :refer [filter-panel build-time-filter]]
            [shevek.viewer.split :refer [split-panel]]
            [shevek.viewer.visualization :refer [visualization-panel]]
            [shevek.viewer.pinboard :refer [pinboard-panels]]))

(defn- init-viewer [{:keys [viewer] :as db} {:keys [measures] :as cube}]
  (-> viewer
      (assoc :filter [(build-time-filter cube)]
             :split []
             :measures (->> measures (take 3) vec)
             :pinboard {:measure (first measures) :dimensions []})
      (->> (assoc db :viewer))))

(defevh :cube-arrived [db {:keys [name] :as cube}]
  (let [cube (dw/set-cube-defaults cube)]
    (-> (assoc-in db [:viewer :cube] cube)
        (init-viewer cube)
        (rpc/loaded :cube-metadata)
        (send-main-query))))

(defevh :cube-selected [db cube]
  (rpc/call "schema.api/cube" :args [cube] :handler #(dispatch :cube-arrived %))
  (dispatch :navigate :viewer)
  (-> (assoc db :viewer {:cube {:name cube}})
      (rpc/loading :cube-metadata)))

(defevh :max-time-arrived [db cube-name max-time]
  (update-in db [:cubes cube-name] assoc :max-time (dw/parse-max-time max-time)))

(defn fetch-max-time []
  (when (= (db/get :page) :viewer)
    (let [name (current-cube-name)]
      (rpc/call "schema.api/max-time" :args [name] :handler #(dispatch :max-time-arrived name %)))))

(defonce _interval (every 60 fetch-max-time))

(defn page []
  [:div#viewer
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
