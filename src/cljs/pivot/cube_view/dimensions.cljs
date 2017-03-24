(ns pivot.cube-view.dimensions
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading-class]]
            [pivot.lib.react :refer [rmap]]
            [pivot.components :refer [controlled-popup]]
            [pivot.cube-view.shared :refer [current-cube panel-header send-main-query]]))

(defn dimension-popup-button [{:keys [close]} color icon event name]
  [:button.ui.circular.icon.button
   {:class color
    :on-click #(do (close) (dispatch event name))}
   [:i.icon {:class icon}]])

; TODO Pivot hace una query para traer la cardinality exacta dependiente de los filtros. La que tengo aca es para toda la historia y es aproximada.
(defn- dimension-popup [popup {:keys [cardinality] :as dim}]
  (let [dim (select-keys dim [:name :title])]
    [:div.ui.special.popup.card {:style {:display (if (popup :opened?) "block" "none")}}
     [:div.content
      [dimension-popup-button popup "green" "filter" :dimension-added-to-filter dim]
      [dimension-popup-button popup "orange" "square" :dimension-replaced-split dim]
      [dimension-popup-button popup "orange" "plus" :dimension-added-to-split dim]
      [dimension-popup-button popup "yellow" "pin" :dimension-pinned dim]]
     (when cardinality
       [:div.extra.content (str cardinality " values")])]))

(defn- type-icon [type name]
  (let [effective-type (if (= name "__time") "TIME" type)]
    (condp = effective-type
      "TIME" "wait"
      "LONG" "hashtag"
      "font")))

(defn- dimension-item [popup {:keys [name title type description] :as dimension}]
  [:div.item {:class (when (popup :opened?) "active") :on-click (popup :toggle) :title description}
   [:i.icon {:class (type-icon type name)}] title])

(defn dimensions-panel []
  [:div.dimensions.panel.ui.basic.segment (loading-class :cube-metadata)
   [panel-header (t :cubes/dimensions)]
   [:div.items
    (rmap (controlled-popup dimension-item dimension-popup {:position "right center" :distanceAway -30})
          (current-cube :dimensions))]])
