(ns pivot.cube
  (:require [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]))

(defn panel-header [t-key]
  [:h2.ui.sub.grey.header (t t-key)])

(defn page []
  (let [cube-name (db/get-in [:params :cube-name])]
    (dispatch :data-requested :dimensions "handler/get-dimensions" cube-name)
    (dispatch :data-requested :measures "handler/get-measures" cube-name)
    (fn []
      [:div#cube
       [:div.left-column
        [:div.dimensions-measures.panel
         [:div.dimensions.section.ui.basic.segment
          {:class (when (loading? :dimensions) "loading")}
          [panel-header :cubes/dimensions]]
         [:div.measures.section.ui.basic.segment
          {:class (when (loading? :measures) "loading")}
          [panel-header :cubes/measures]]]]
       [:div.center-column
        [:div.filters-splits.panel
         [:div.filters.section
          [panel-header :cubes/filters]]
         [:div.section
          [panel-header :cubes/split]]]
        [:div.visualization.panel.section "Content"]]
       [:div.right-column
        [:div.pinboard.panel.section
         [panel-header :cubes/pinboard]]]])))
