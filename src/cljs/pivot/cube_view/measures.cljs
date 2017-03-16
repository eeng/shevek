(ns pivot.cube-view.measures
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [pivot.i18n :refer [t]]
            [pivot.rpc :refer [loading?]]
            [pivot.lib.react :refer [rmap]]
            [pivot.rpc :as rpc]
            [pivot.components :refer [checkbox]]
            [pivot.cube-view.shared :refer [current-cube cube-view panel-header add-dimension remove-dimension send-main-query includes-dim?]]))

(defevh :measure-toggled [db dim selected]
  (-> (update-in db [:cube-view :measures] (if selected add-dimension remove-dimension) dim)
      (send-main-query)))

(defn- measure-item [{:keys [title] :as dim}]
  [:div.item {:on-click #(-> % .-target js/$ (.find ".checkbox input") .click)}
   [checkbox title {:checked (includes-dim? (cube-view :measures) dim)
                    :on-change #(dispatch :measure-toggled dim %)}]])

(defn- measures-panel []
  ^{:key (cube-view :cube)}
  [:div.measures.panel.ui.basic.segment (rpc/loading-class :cube-metadata)
   [panel-header (t :cubes/measures)]
   [:div.items
    (rmap measure-item (current-cube :measures))]])
