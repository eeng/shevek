(ns pivot.cube-view.page
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [pivot.rpc :as rpc]
            [pivot.dw :as dw]
            [pivot.cube-view.shared :refer [send-main-query]]
            [pivot.cube-view.dimensions :refer [dimensions-panel]]
            [pivot.cube-view.measures :refer [measures-panel]]
            [pivot.cube-view.filter :refer [filter-panel]]
            [pivot.cube-view.split :refer [split-panel]]
            [pivot.cube-view.visualization :refer [visualization-panel]]
            [pivot.cube-view.pinboard :refer [pinboard-panel]]))

;; DB model example
#_{:cubes {"wikiticker"
           {:dimensions [{:name "page"}]
            :measures [{:name "added"} {:name "deleted"}]
            :time-boundary {:max-time "..."}}}
   :cube-view {:cube "wikitiker"
               :filter [{:name "__time" :selected-period :latest-day}]
               :split [{:name "region"}]
               :measures [{:name "added"}] ; Selected measures in the left panel
               :pinboard {:measure "channel" ; Selected measure for the pinboard
                          :dimensions [{:name "channel"}]} ; Pinned dimensions
               :results {:main {...druid response...}
                         :pinboard {"dim1-name" {...druid response...}
                                    "dim2-name" {...druid response...}}}}}

(defn- build-time-filter [{:keys [dimensions time-boundary] :as cube}]
  (assoc (dw/time-dimension dimensions)
         :max-time (:max-time time-boundary)
         :selected-period :latest-day))

(defn- init-cube-view [{:keys [cube-view] :as db} {:keys [measures] :as cube}]
  (-> cube-view
      (assoc :filter [(build-time-filter cube)]
             :split []
             :measures (->> measures (take 3) vec)
             :pinboard {:measure (first measures)})
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
    [:div.dimensions-measures.zone
     [dimensions-panel]
     [measures-panel]]]
   [:div.center-column
    [:div.zone
     [filter-panel]
     [split-panel]]
    [visualization-panel]]
   [:div.right-column
    [pinboard-panel]]])
