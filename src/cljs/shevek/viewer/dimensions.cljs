(ns shevek.viewer.dimensions
  (:require-macros [shevek.reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :refer [loading-class]]
            [shevek.components.popup :refer [show-popup close-popup popup-opened?]]
            [shevek.components.drag-and-drop :refer [draggable]]
            [shevek.viewer.shared :refer [current-cube panel-header send-main-query filter-matching search-button search-input highlight]]))

(defn dimension-popup-button [color icon event name]
  [:button.ui.circular.icon.button
   {:class color
    :on-click #(do (close-popup) (dispatch event name))}
   [:i.icon {:class icon}]])

(defn- dimension-popup [dim]
  [:div.popup-content
   [:div.buttons
    [dimension-popup-button "green" "filter" :dimension-added-to-filter dim]
    [dimension-popup-button "orange" "square" :split-replaced dim]
    [dimension-popup-button "orange" "plus" :split-dimension-added dim]
    [dimension-popup-button "yellow" "pin" :dimension-pinned dim]]])

(defn- type-icon [type name]
  (let [effective-type (if (= name "__time") "TIME" type)]
    (condp = effective-type
      "TIME" "wait"
      "LONG" "hashtag"
      "font")))

(defn- dimension-item [search {:keys [name title type description] :as dim}]
  [:div.item (assoc (draggable dim)
                    :title description
                    :class (when (popup-opened? name) "active")
                    :on-click #(show-popup % [dimension-popup dim] {:position "right center" :distanceAway -30 :id name}))
   [:i.icon {:class (type-icon type name)}]
   (highlight title search)])

(defn dimensions-panel []
  (let [searching (r/atom false)
        search (r/atom "")]
    (fn []
      (let [filtered-dims (->> (current-cube :dimensions)
                               (filter-matching @search :title)
                               (sort-by :title))
            search-text @search]
        [:div.dimensions.panel.ui.basic.segment (loading-class :cube-metadata)
         [panel-header (t :viewer/dimensions) [search-button searching]]
         (when @searching
           [search-input search {:on-stop #(reset! searching false)}])
         [:div.items
          (for [dim filtered-dims]
            ^{:key (:name dim)} [dimension-item search-text dim])]]))))
