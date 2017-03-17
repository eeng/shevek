(ns pivot.cube-view.dimensions
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.components :refer [popup]]
            [pivot.cube-view.shared :refer [current-cube panel-header add-dimension remove-dimension send-main-query]]))

(defevh :dimension-added-to-filter [db dim]
  (-> (update-in db [:cube-view :filter] add-dimension dim)
      #_(send-main-query)))

(defevh :dimension-removed-from-filter [db dim]
  (-> (update-in db [:cube-view :filter] remove-dimension dim)
      #_(send-main-query)))

(defevh :dimension-added-to-split [db dim]
  (-> (update-in db [:cube-view :split] add-dimension dim)
      #_(send-main-query)))

(defevh :dimension-replaced-split [db dim]
  (-> (assoc-in db [:cube-view :split] [dim])
      #_(send-main-query)))

(defevh :dimension-removed-from-split [db dim]
  (-> (update-in db [:cube-view :split] remove-dimension dim)
      #_(send-main-query)))

(defn dimension-popup-button [color icon event selected name]
  [:button.ui.circular.icon.button
   {:class color
    :on-click #(do (reset! selected false)
                 (dispatch event name))}
   [:i.icon {:class icon}]])

; TODO Pivot hace una query para traer la cardinality, supongo q x si se actualiza desde que se trajo toda la metadata
(defn- dimension-popup [{:keys [cardinality] :as dim} selected]
  (let [dim (select-keys dim [:name :title])]
    [:div.ui.special.popup.card {:style {:display (if @selected "block" "none")}}
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

(defn- dimension-item* [selected {:keys [name title type description] :as dimension}]
  [popup
   [:div.item.dimension {:on-click #(swap! selected not)
                         :class (when @selected "active")
                         :title description}
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

(defn dimensions-panel []
  [:div.dimensions.panel.ui.basic.segment (rpc/loading-class :cube-metadata)
   [panel-header (t :cubes/dimensions)]
   [:div.items
    (rmap dimension-item (current-cube :dimensions))]])
