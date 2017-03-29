(ns shevek.cube-view.page
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.dw :as dw]
            [shevek.cube-view.shared :refer [send-main-query clean-dim]]
            [shevek.cube-view.dimensions :refer [dimensions-panel]]
            [shevek.cube-view.measures :refer [measures-panel]]
            [shevek.cube-view.filter :refer [filter-panel init-filtered-dim]]
            [shevek.cube-view.split :refer [split-panel]]
            [shevek.cube-view.visualization :refer [visualization-panel]]
            [shevek.cube-view.pinboard :refer [pinboard-panels]]))

(defn- build-time-filter [{:keys [dimensions time-boundary] :as cube}]
  (assoc (clean-dim (dw/time-dimension dimensions))
         :max-time (:max-time time-boundary)
         :selected-period :latest-day))

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
    (-> (assoc-in db [:cubes name] cube)
        (init-cube-view cube)
        (rpc/loaded :cube-metadata)
        (send-main-query))))

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
