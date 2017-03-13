(ns pivot.cube
  (:require [reagent.core :as r]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.react :refer [rmap]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [pivot.components :refer [checkbox popup]]))

(defn current-cube-name []
  (db/get-in [:params :current-cube]))

(defn current-cube []
  (some #(when (= (current-cube-name) (:name %)) %)
        (db/get :cubes)))

(defn- panel-header [t-key]
  [:h2.ui.sub.header (t t-key)])

(defn- dimension-popup [{:keys [name cardinality description]}]
  [:div.ui.special.popup.card
   (when description
     [:div.content
      [:div.description description]])
   [:div.content
    [:div
     [:button.ui.circular.icon.blue.button [:i.filter.icon]]
     [:button.ui.circular.icon.green.button [:i.square.icon]]
     [:button.ui.circular.icon.orange.button [:i.plus.icon]]
     [:button.ui.circular.icon.yellow.button [:i.pin.icon]]]]
   (when cardinality
     [:div.extra.content (str cardinality " values")])])

(defn- dimension-item* [selected {:keys [name title] :as dimension}]
  [:div.item {:on-click #(swap! selected not)
              :class (when @selected "active")}
   [:i.font.icon] title])

(defn- dimension-item [{:keys [name] :as dimension}]
  (let [selected (r/atom false)
        handle-click-outside (fn [dim-item e]
                               (when-not (.contains (r/dom-node dim-item) (.-target e))
                                 (reset! selected false)))
        node-listener (atom nil)]
    (r/create-class {:reagent-render (partial dimension-item* selected)
                     :component-did-mount #(do
                                             (reset! node-listener (partial handle-click-outside %))
                                             (.addEventListener js/document "click" @node-listener true))
                     :component-will-unmount #(.removeEventListener js/document "click" @node-listener true)})))

(defn- dimensions-panel []
  [:div.dimensions.panel.ui.basic.segment
   [panel-header :cubes/dimensions]
   [:div.items
    (rmap dimension-item (:dimensions (current-cube)))]])

(defn- measure-item [{:keys [name title]}]
  [:div.item {:on-click console.log}
   [checkbox title]])

(defn- measures-panel []
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
      [panel-header :cubes/filter]]
     [:div.panel
      [panel-header :cubes/split]]]
    [:div.visualization.zone.panel]]
   [:div.right-column
    [:div.pinboard.zone.panel
     [panel-header :cubes/pinboard]]]])
