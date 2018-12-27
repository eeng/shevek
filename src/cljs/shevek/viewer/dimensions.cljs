(ns shevek.viewer.dimensions
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :refer [loading-class]]
            [shevek.components.popup :refer [show-popup close-popup]]
            [shevek.components.drag-and-drop :refer [draggable]]
            [shevek.components.form :refer [search-input filter-matching]]
            [shevek.viewer.shared :refer [current-cube panel-header send-main-query search-button highlight description-help-icon]]))

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

(defn- dimension-item []
  (let [selected (r/atom false)]
    (fn [search {:keys [title] :as dim}]
      [:div.item (assoc (draggable dim)
                        :class (when @selected "active")
                        :on-click #(show-popup % [dimension-popup dim]
                                               {:on-toggle (partial reset! selected)
                                                :position "right center"
                                                :distanceAway -30}))
       [:i.icon {:class "genderless"}]
       [:span (highlight title search) [description-help-icon dim]]])))

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
           [search-input search {:on-stop #(reset! searching false) :wrapper {:class "small"}}])
         [:div.items
          (for [dim filtered-dims]
            ^{:key (:name dim)} [dimension-item search-text dim])]]))))
