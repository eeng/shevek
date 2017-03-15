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

;; DB model example
#_{:cubes {"wikiticker"
           {:dimensions [{:name "page"}]
            :measures [{:name "added"} {:name "deleted"}]
            :time-boundary {:max-time "..."}}}
   :cube-view {:cube "wikitiker"
               :filter [{:name "__time" :selected-period :latest-day}]
               :split [{:name "region"}]
               :measures [{:name "added"}]
               :pinned [{:name "channel"}]
               :main-results [{:timestamp "..." :result []}]}}

(defevh :query-executed [db results]
  (-> (assoc-in db [:cube-view :main-results] results)
      (rpc/loaded :main-results)))

(defn- build-time-filter [{:keys [dimensions time-boundary] :as cube}]
  (assoc (dw/time-dimension dimensions)
         :max-time (:max-time time-boundary)
         :selected-period :latest-day))

(defn- init-cube-view [{:keys [cube-view] :as db} cube]
  (-> cube-view
      (assoc :filter [(build-time-filter cube)])
      (assoc :split [])
      (->> (assoc db :cube-view))))

(defn- send-query [{:keys [cube-view] :as db}]
  (rpc/call "dw/query" :args [(dw/to-dw-query cube-view)] :handler #(dispatch :query-executed %))
  (rpc/loading db :query-results))

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
        (send-query))))

(defn add-dimension [coll dim]
  (let [coll (or coll [])]
    (if (some #(dw/dim=? % dim) coll)
      coll
      (conj coll dim))))

(defn remove-dimension [coll dim]
  (vec (remove #(dw/dim=? dim %) coll)))

(defevh :dimension-added-to-filter [db dim]
  (-> (update-in db [:cube-view :filter] add-dimension dim)
      (send-query)))

(defevh :dimension-removed-from-filter [db dim]
  (-> (update-in db [:cube-view :filter] remove-dimension dim)
      (send-query)))

(defevh :dimension-added-to-split [db dim]
  (-> (update-in db [:cube-view :split] add-dimension dim)
      (send-query)))

(defevh :dimension-replaced-split [db dim]
  (-> (assoc-in db [:cube-view :split] [dim])
      (send-query)))

(defevh :dimension-removed-from-split [db dim]
  (-> (update-in db [:cube-view :split] remove-dimension dim)
      (send-query)))

(defevh :dimension-pinned [db dim]
  (update-in db [:cube-view :pinned] add-dimension dim))

(defevh :measure-toggled [db dim selected]
  (-> (update-in db [:cube-view :measures] (if selected add-dimension remove-dimension) dim)
      (send-query)))

(defn- cube-view-get [key]
  (db/get-in [:cube-view key]))

(defn current-cube-name []
  (cube-view-get :cube))

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
  [:div.dimensions.panel.ui.basic.segment {:class (when (rpc/loading? :cube-metadata) "loading")}
   [panel-header :cubes/dimensions]
   [:div.items
    (rmap dimension-item (:dimensions (current-cube)))]])

(defn- measure-item [{:keys [title] :as dim}]
  [:div.item {:on-click #(-> % .-target js/$ (.find ".checkbox input") .click)}
   [checkbox title {:on-change #(dispatch :measure-toggled dim %)}]])

(defn- measures-panel []
  ^{:key (current-cube-name)}
  [:div.measures.panel.ui.basic.segment {:class (when (rpc/loading? :cube-metadata) "loading")}
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
   (rmap filter-item (:filter (db/get :cube-view)))])

(defn- split-item [{:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button
   [:i.close.icon {:on-click #(dispatch :dimension-removed-from-split dim)}]
   title])

(defn- split-panel []
  [:div.split.panel
   [panel-header :cubes/split]
   (rmap split-item (:split (db/get :cube-view)))])

(defn- pinboard-panel []
  [:div.pinboard.zone
   [panel-header :cubes/pinboard]
   (if (seq (cube-view-get :pinned))
     [:div.panel.ui.basic.segment "Uno de estos por cada pinned dim"]
     [:div.panel.ui.basic.segment.no-pinned
      [:div.ui.icon.header
       [:i.pin.icon]
       [:div.text (t :cubes/no-pinned)]]])])

(defn- visualization-panel []
  [:div.visualization.zone.panel
   (if (empty? (cube-view-get :measures))
     [:div "Please select at least one measure"]
     (if (empty? (cube-view-get :split))
       [:div "TODO totals"]
       [:div "TODO lista"]))])

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
