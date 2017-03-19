(ns pivot.cube-view.filter-split
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.dw :as dw]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.cube-view.shared :refer [panel-header cube-view]]
            [pivot.components :refer [with-controlled-popup]]))

(defn- filter-popup [selected dim]
  [:div.ui.special.popup.card {:style {:display (if @selected "block" "none")}}
   [:div.content "popup for" (str dim)]])

(defn- filter-item [selected {:keys [title] :as dim}]
  [:button.ui.green.compact.button.item
   {:class (when-not (dw/time-dimension? dim) "right labeled icon")
    :on-click #(swap! selected not)}
   (when-not (dw/time-dimension? dim)
     [:i.close.icon {:on-click #(dispatch :dimension-removed-from-filter dim)}])
   title])

(defn filter-panel []
  [:div.filter.panel
   [panel-header (t :cubes/filter)]
   (rmap (with-controlled-popup filter-item filter-popup {:position "bottom center"}) (cube-view :filter))])

(defn- split-item [{:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button
   [:i.close.icon {:on-click #(dispatch :dimension-removed-from-split dim)}]
   title])

(defn split-panel []
  [:div.split.panel
   [panel-header (t :cubes/split)]
   (rmap split-item (cube-view :split))])
