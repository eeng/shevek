(ns pivot.cube-view.filter-split
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.dw :as dw]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.cube-view.shared :refer [panel-header cube-view]]))

(defn- filter-item [{:keys [title] :as dim}]
  [:button.ui.green.compact.button {:class (when-not (dw/time-dimension? dim) "right labeled icon")}
   (when-not (dw/time-dimension? dim)
     [:i.close.icon {:on-click #(dispatch :dimension-removed-from-filter dim)}])
   title])

(defn filter-panel []
  [:div.filter.panel
   [panel-header (t :cubes/filter)]
   (rmap filter-item (cube-view :filter))])

(defn- split-item [{:keys [title] :as dim}]
  [:button.ui.orange.compact.right.labeled.icon.button
   [:i.close.icon {:on-click #(dispatch :dimension-removed-from-split dim)}]
   title])

(defn split-panel []
  [:div.split.panel
   [panel-header (t :cubes/split)]
   (rmap split-item (cube-view :split))])
