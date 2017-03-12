(ns pivot.cube
  (:require [reagent.core :as r]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.react :refer [rmap]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [pivot.components :refer [checkbox]]))

(defn panel-header [t-key]
  [:h2.ui.sub.header (t t-key)])

(defn current-cube-name []
  (db/get-in [:params :current-cube]))

(defn current-cube []
  (some #(when (= (current-cube-name) (:name %)) %)
        (db/get :cubes)))

(defn dimension-item [selected {:keys [name title cardinality]}]
  (let [toggle-selected #(if (= % name) nil name)]
    [:div.item {:on-click #(swap! selected toggle-selected)
                :class (when (= @selected name) "active")}
     [:i.font.icon] title]))

(defn dimensions-panel []
  (let [selected (r/atom nil)]
    (fn []
      [:div.dimensions.panel.ui.basic.segment
       [panel-header :cubes/dimensions]
       [:div.items
        (rmap (partial dimension-item selected) (:dimensions (current-cube)))]])))

(defn measure-item [{:keys [name title]}]
  [:div.item {:on-click console.log}
   [checkbox title]])

(defn measures-panel []
  [:div.measures.panel.ui.basic.segment
   [panel-header :cubes/measures]
   [:div.items
    (rmap measure-item (:measures (current-cube)))]])

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
