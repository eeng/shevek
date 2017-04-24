(ns shevek.viewer.measures
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [reagent.core :as r]
            [reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :refer [loading?]]
            [shevek.rpc :as rpc]
            [shevek.components :refer [checkbox toggle-checkbox-inside]]
            [shevek.dw :refer [add-dimension remove-dimension includes-dim?]]
            [shevek.viewer.shared :refer [current-cube viewer panel-header send-main-query]]
            [shevek.reports.url :refer [store-in-url]]))

(defevh :measure-toggled [db dim selected]
  (-> (update-in db [:viewer :measures] (if selected add-dimension remove-dimension) dim)
      (send-main-query)))

(defn- measure-item [{:keys [title description] :as dim} selected-measures]
  [:div.item {:on-click toggle-checkbox-inside :title description}
   [checkbox title {:checked (includes-dim? selected-measures dim)
                    :on-change #(dispatch :measure-toggled dim %)}]])

(defn- measures-panel []
  (let [selected-measures (viewer :measures)]
    [:div.measures.panel.ui.basic.segment (rpc/loading-class :cube-metadata)
     [panel-header (t :cubes/measures)]
     [:div.items
      (for [m (sort-by :title (current-cube :measures))]
        ^{:key (:name m)} [measure-item m selected-measures])]]))
