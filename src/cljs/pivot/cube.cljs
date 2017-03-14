(ns pivot.cube
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.dw :as dw]
            [reflow.db :as db]
            [reflow.core :refer [dispatch]]
            [cuerdas.core :as str]
            [pivot.components :refer [checkbox popup]]))

(defevh :cube-selected [db cube]
  (rpc/call "dw/cube" :args [cube] :handler #(dispatch :cube-arrived %))
  (dispatch :navigate :cube)
  (-> (assoc db :query {:cube cube})
      (rpc/loading :cube)))

(defevh :query-executed [db results]
  (-> (assoc db :results results)
      (rpc/loaded :query)))

(defn- build-time-filter [{:keys [dimensions time-boundary] :as cube}]
  (assoc (dw/time-dimension dimensions)
         :max-time (:max-time time-boundary)
         :selected-period :latest-day))

(defn- init-query [{:keys [query] :as db} cube]
  (-> query
      (assoc :filter [(build-time-filter cube)])
      (assoc :split [])
      (->> (assoc db :query))))

(defn- send-query [{:keys [query] :as db}]
  (rpc/call "dw/query" :args [(dw/to-dw-query query)] :handler #(dispatch :query-executed %))
  (rpc/loading db :query))

(defevh :cube-arrived [db {:keys [name] :as cube}]
  (let [cube (dw/set-cube-defaults cube)]
    (-> (assoc-in db [:cubes name] cube)
        (init-query cube)
        (rpc/loaded :cube)
        (send-query))))

(defn add-dimension [coll dim]
  (let [coll (or coll [])]
    (if (some #(dw/dim=? % dim) coll)
      coll
      (conj coll dim))))

(defn remove-dimension [coll dim]
  (vec (remove #(dw/dim=? dim %) coll)))

(defevh :dimension-added-to-filter [db dim]
  (-> (update-in db [:query :filter] add-dimension dim)
      (send-query)))

(defevh :dimension-removed-from-filter [db dim]
  (-> (update-in db [:query :filter] remove-dimension dim)
      (send-query)))

(defevh :dimension-added-to-split [db dim]
  (-> (update-in db [:query :split] add-dimension dim)
      (send-query)))

(defevh :dimension-replaced-split [db dim]
  (-> (assoc-in db [:query :split] [dim])
      (send-query)))

(defevh :dimension-removed-from-split [db dim]
  (-> (update-in db [:query :split] remove-dimension dim)
      (send-query)))

(defevh :dimension-pinned [db dim]
  (update-in db [:query :pinned] add-dimension dim))

(defevh :measure-toggled [db name selected]
  (-> (update-in db [:query :measures] (fnil (if selected conj disj) #{}) name)
      (send-query)))

(defn current-cube-name []
  (db/get-in [:query :cube]))

(defn current-cube []
  (get (db/get :cubes) (current-cube-name)))

(defn- panel-header [t-key]
  [:h2.ui.sub.header (t t-key)])

(defn dimension-popup-button [color icon event selected name]
  [:button.ui.circular.icon.button
   {:class color
    :on-click #(do (reset! selected false)
                 (dispatch event name))}
   [:i.icon {:class icon}]])

; TODO Pivot hace una query para traer la cardinality, supongo q x si se actualiza desde que se trajo toda la metadata
(defn- dimension-popup [{:keys [cardinality description] :as dim} selected]
  (let [dim (select-keys dim [:name :title])]
    [:div.ui.special.popup.card {:style {:display (if @selected "block" "none")}}
     (when description
       [:div.content
        [:div.description description]])
     [:div.content
      [dimension-popup-button "green" "filter" :dimension-added-to-filter selected dim]
      [dimension-popup-button "orange" "square" :dimension-replaced-split selected dim]
      [dimension-popup-button "orange" "plus" :dimension-added-to-split selected dim]
      [dimension-popup-button "yellow" "pin" :dimension-pinned selected dim]]
     (when cardinality
       [:div.extra.content (str cardinality " values")])]))

(defn- type-icon [type name]
  (let [effective-type (if (= name "__time") "TIME" type)]
    (condp = effective-type
      "TIME" "wait"
      "LONG" "hashtag"
      "font")))

(defn- dimension-item* [selected {:keys [name title type] :as dimension}]
  [popup
   [:div.item.dimension {:on-click #(swap! selected not)
                         :class (when @selected "active")}
    [:i.icon {:class (type-icon type name)}] title]
   [dimension-popup dimension selected]
   {:on "manual" :position "right center" :distanceAway -30}])

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
  [:div.dimensions.panel.ui.basic.segment {:class (when (rpc/loading? :cube) "loading")}
   [panel-header :cubes/dimensions]
   [:div.items
    (rmap dimension-item (:dimensions (current-cube)))]])

(defn- measure-item [{:keys [name title]}]
  [:div.item {:on-click #(-> % .-target js/$ (.find ".checkbox input") .click)}
   [checkbox title {:on-change #(dispatch :measure-toggled name %)}]])

(defn- measures-panel []
  ^{:key (current-cube-name)}
  [:div.measures.panel.ui.basic.segment {:class (when (rpc/loading? :cube) "loading")}
   [panel-header :cubes/measures]
   [:div.items
    (rmap measure-item (:measures (current-cube)))]])

(defn- filter-item [{:keys [title] :as dim}]
  [:button.ui.green.compact.button {:class (when-not (dw/time-dimension? dim) "right labeled icon")}
   (when-not (dw/time-dimension? dim)
     [:i.close.icon {:on-click #(dispatch :dimension-removed-from-filter dim)}])
   title])


(defn- filter-panel []
  [:div.filter.panel
   [panel-header :cubes/filter]
   (rmap filter-item (:filter (db/get :query)))])

(defn- split-item [{:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button
   [:i.close.icon {:on-click #(dispatch :dimension-removed-from-split dim)}]
   title])

(defn- split-panel []
  [:div.split.panel
   [panel-header :cubes/split]
   (rmap split-item (:split (db/get :query)))])

(defn- pinboard-panel []
  [:div.pinboard.zone
   [panel-header :cubes/pinboard]
   (if (seq (db/get-in [:query :pinned]))
     [:div.panel.ui.basic.segment "Uno de estos por cada pinned dim"]
     [:div.panel.ui.basic.segment.no-pinned
      [:div.ui.icon.header
       [:i.pin.icon]
       [:div.text (t :cubes/no-pinned)]]])])

(defn page []
  [:div#cube
   [:div.left-column
    [:div.dimensions-measures.zone
     [dimensions-panel]
     [measures-panel]]]
   [:div.center-column
    [:div.zone
     [filter-panel]
     [split-panel]]
    [:div.visualization.zone.panel]]
   [:div.right-column
    [pinboard-panel]]])
