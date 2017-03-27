(ns shevek.cube-view.page
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [shevek.rpc :as rpc]
            [shevek.dw :as dw]
            [shevek.cube-view.shared :refer [send-main-query]]
            [shevek.cube-view.dimensions :refer [dimensions-panel]]
            [shevek.cube-view.measures :refer [measures-panel]]
            [shevek.cube-view.filter :refer [filter-panel]]
            [shevek.cube-view.split :refer [split-panel]]
            [shevek.cube-view.visualization :refer [visualization-panel]]
            [shevek.cube-view.pinboard :refer [pinboard-panels]]))

;; DB model example
#_{:cubes {"wikiticker"
           {:dimensions [{:name "page"}]
            :measures [{:name "added"} {:name "deleted"}]
            :time-boundary {:max-time "..."}}}
   :cube-view {:cube "wikitiker"
               :filter [{:name "__time" :selected-period :latest-day}
                        {:name "countryName" :include ["Italy"]}]
               :split [{:name "region" :sort-by {:name "count" :descending true}}]
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
    [dimensions-panel]
    [measures-panel]]
   [:div.center-column
    [:div
     [filter-panel]
     [split-panel]]
    [visualization-panel]]
   [:div.right-column
    [pinboard-panels]]])
