(ns shevek.cube-view.measures
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :refer [loading?]]
            [shevek.rpc :as rpc]
            [shevek.components :refer [checkbox toggle-checkbox-inside]]
            [shevek.dw :refer [add-dimension remove-dimension includes-dim?]]
            [shevek.cube-view.shared :refer [current-cube cube-view panel-header send-main-query]]))

(defevh :measure-toggled [db dim selected]
  (cond-> (update-in db [:cube-view :measures] (if selected add-dimension remove-dimension) dim)
          selected (send-main-query)))

(defn- measure-item [{:keys [title description] :as dim} selected-measures]
  [:div.item {:on-click toggle-checkbox-inside :title description}
   [checkbox title {:checked (includes-dim? selected-measures dim)
                    :on-change #(dispatch :measure-toggled dim %)}]])

(defn- measures-panel []
  (let [selected-measures (cube-view :measures)]
    [:div.measures.panel.ui.basic.segment (rpc/loading-class :cube-metadata)
     [panel-header (t :cubes/measures)]
     [:div.items
      (for [m (current-cube :measures)]
        ^{:key (:name m)} [measure-item m selected-measures])]]))
