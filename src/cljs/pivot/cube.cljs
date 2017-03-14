(ns pivot.cube
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.react :refer [rmap]]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [pivot.components :refer [checkbox popup]]))

(defevh :cube-selected [db cube]
  (dispatch :navigate :cube)
  (assoc db :query {:cube cube}))

(defevh :measure-toggled [db name selected]
  (update-in db [:query :measures] (fnil (if selected conj disj) #{}) name))

(defevh :dimension-added-to-filter [db name]
  db)

(defevh :dimension-added-to-split [db name]
  db)

(defevh :dimension-replaced-split [db name]
  db)

(defevh :dimension-pinned [db name]
  (update-in db [:query :pinned] (fnil conj #{}) name))

(defn current-cube-name []
  (db/get-in [:query :cube]))

(defn current-cube []
  (some #(when (= (current-cube-name) (:name %)) %)
        (db/get :cubes)))

(defn- panel-header [t-key]
  [:h2.ui.sub.header (t t-key)])

(defn dimension-popup-button [color icon event selected name]
  [:button.ui.circular.icon.button
   {:class color
    :on-click #(do (reset! selected false)
                 (dispatch event name))}
   [:i.icon {:class icon}]])

; TODO Pivot hace una query para traer la cardinality, supongo q x si se actualiza desde que se trajo toda la metadata
(defn- dimension-popup [{:keys [name cardinality description]} selected]
  [:div.ui.special.popup.card {:style {:display (if @selected "block" "none")}}
   (when description
     [:div.content
      [:div.description description]])
   [:div.content
    [dimension-popup-button "blue" "filter" :dimension-added-to-filter selected name]
    [dimension-popup-button "green" "square" :dimension-replaced-split selected name]
    [dimension-popup-button "orange" "plus" :dimension-added-to-split selected name]
    [dimension-popup-button "yellow" "pin" :dimension-pinned selected name]]
   (when cardinality
     [:div.extra.content (str cardinality " values")])])

(defn- dimension-item* [selected {:keys [name title] :as dimension}]
  [popup
   [:div.item.dimension {:on-click #(swap! selected not)
                         :class (when @selected "active")}
    [:i.font.icon] title]
   [dimension-popup dimension selected]
   {:on "manual" :position "right center" :distanceAway -20}])

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
  [:div.item {:on-click #(-> % .-target js/$ (.find ".checkbox input") .click)}
   [checkbox title {:on-change #(dispatch :measure-toggled name %)}]])

(defn- measures-panel []
  ^{:key (current-cube-name)}
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
