(ns pivot.cube
  (:require [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]))

(defn panel-header [t-key]
  [:h2.ui.sub.grey.header (t t-key)])

(defn current-cube-name []
  (db/get-in [:params :current-cube]))

(defn current-cube []
  (some #(when (= (current-cube-name) (:name %)) %)
        (db/get :cubes)))

(defn dimension-item [{:keys [name title cardinality] :or {title (str/title name)}}]
  ^{:key name}
  [:div.item {:on-click console.log}
   [:i.font.icon] title])

(defn dimensions-section []
  (let [dimensions (:dimensions (current-cube))]
    [:div.dimensions.section.ui.basic.segment
     [panel-header :cubes/dimensions]
     [:div.items
      (map dimension-item dimensions)]]))

(defn measures-section []
  [:div.measures.section.ui.basic.segment
   [panel-header :cubes/measures]])

(defn page []
  [:div#cube
   [:div.left-column
    [:div.dimensions-measures.panel
     [dimensions-section]
     [measures-section]]]
   [:div.center-column
    [:div.filters-splits.panel
     [:div.filters.section
      [panel-header :cubes/filters]]
     [:div.section
      [panel-header :cubes/split]]]
    [:div.visualization.panel.section "Content"]]
   [:div.right-column
    [:div.pinboard.panel.section
     [panel-header :cubes/pinboard]]]])
