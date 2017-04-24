(ns shevek.viewer.dimensions
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :refer [loading-class]]
            [shevek.components :refer [controlled-popup]]
            [shevek.lib.react :refer [rmap]]
            [shevek.viewer.shared :refer [current-cube panel-header send-main-query filter-matching search-button search-input highlight]]))

(defn dimension-popup-button [{:keys [close]} color icon event name]
  [:button.ui.circular.icon.button
   {:class color
    :on-click #(do (close) (dispatch event name))}
   [:i.icon {:class icon}]])

; TODO Pivot hace una query para traer la cardinality exacta dependiente de los filtros. La que tengo aca es para toda la historia y es aproximada.
(defn- dimension-popup [popup {:keys [cardinality] :as dim}]
  [:div.popup-content
   [:div.buttons
    [dimension-popup-button popup "green" "filter" :dimension-added-to-filter dim]
    [dimension-popup-button popup "orange" "square" :dimension-replaced-split dim]
    [dimension-popup-button popup "orange" "plus" :dimension-added-to-split dim]
    [dimension-popup-button popup "yellow" "pin" :dimension-pinned dim]]
   (when cardinality
     [:div.details (str cardinality " values")])])

(defn- type-icon [type name]
  (let [effective-type (if (= name "__time") "TIME" type)]
    (condp = effective-type
      "TIME" "wait"
      "LONG" "hashtag"
      "font")))

(defn- dimension-item [search popup {:keys [name title type description] :as dimension}]
  [:div.item {:class (when (popup :opened?) "active") :on-click (popup :toggle) :title description}
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
         [panel-header (t :cubes/dimensions) [search-button searching]]
         (when @searching
           [search-input search {:on-stop #(reset! searching false)}])
         [:div.items
          (rmap (controlled-popup (partial dimension-item search-text) dimension-popup
                                  {:position "right center" :distanceAway -30})
                filtered-dims
                :name)]]))))
