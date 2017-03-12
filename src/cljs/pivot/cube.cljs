(ns pivot.cube
  (:require [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]))

(defn panel-header [t-key]
  [:h2.ui.sub.header (t t-key)])

(defn current-cube-name []
  (db/get-in [:params :current-cube]))

(defn current-cube []
  (some #(when (= (current-cube-name) (:name %)) %)
        (db/get :cubes)))

(defn dimension-item [{:keys [name title cardinality]}]
  ^{:key name}
  [:div.item {:on-click console.log}
   [:i.font.icon] title])

(defn dimensions-panel []
  (let [dimensions (:dimensions (current-cube))]
    [:div.dimensions.panel.ui.basic.segment
     [panel-header :cubes/dimensions]
     [:div.items
      (map dimension-item dimensions)]]))

(defn measures-panel []
  [:div.measures.panel.ui.basic.segment
   [panel-header :cubes/measures]])

(defn page []
  [:div#cube
   [:div.left-column
    [:div.dimensions-measures.zone
     [dimensions-panel]
     [measures-panel]]]
   [:div.center-column
    [:div.filters-splits.zone
     [:div.filters.panel
      [panel-header :cubes/filters]]
     [:div.panel
      [panel-header :cubes/split]]]
    [:div.visualization.zone.panel "Content"]]
   [:div.right-column
    [:div.pinboard.zone.panel
     [panel-header :cubes/pinboard]]]])
