(ns pivot.cube-view.dimensions
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.components :refer [with-controlled-popup]]
            [pivot.dw :refer [time-dimension? add-dimension remove-dimension]]
            [pivot.cube-view.shared :refer [current-cube panel-header send-main-query]]))

(defn- set-split-defaults [dim]
  (cond-> dim
          (time-dimension? dim) (assoc :granularity "PT1H")))

(defevh :dimension-added-to-filter [db dim]
  (-> (update-in db [:cube-view :filter] add-dimension dim)
      #_(send-main-query)))

(defevh :dimension-removed-from-filter [db dim]
  (-> (update-in db [:cube-view :filter] remove-dimension dim)
      #_(send-main-query)))

(defevh :dimension-added-to-split [db dim]
  (-> (update-in db [:cube-view :split] add-dimension (set-split-defaults dim))
      (send-main-query)))

(defevh :dimension-replaced-split [db dim]
  (-> (assoc-in db [:cube-view :split] [(set-split-defaults dim)])
      (send-main-query)))

(defevh :dimension-removed-from-split [db dim]
  (-> (update-in db [:cube-view :split] remove-dimension dim)
      (send-main-query)))

(defn dimension-popup-button [color icon event selected name]
  [:button.ui.circular.icon.button
   {:class color
    :on-click #(do (reset! selected false)
                 (dispatch event name))}
   [:i.icon {:class icon}]])

; TODO Pivot hace una query para traer la cardinality exacta dependiente de los filtros. La que tengo aca es para toda la historia y es aproximada.
(defn- dimension-popup [selected {:keys [cardinality] :as dim}]
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

(defn- dimension-item [selected {:keys [name title type description] :as dimension}]
  [:div.item {:class (when @selected "active")
              :on-click #(swap! selected not)
              :title description}
   [:i.icon {:class (type-icon type name)}] title])

(defn dimensions-panel []
  [:div.dimensions.panel.ui.basic.segment (rpc/loading-class :cube-metadata)
   [panel-header (t :cubes/dimensions)]
   [:div.items
    (rmap (with-controlled-popup dimension-item dimension-popup {:position "right center" :distanceAway -30})
          (current-cube :dimensions))]])
