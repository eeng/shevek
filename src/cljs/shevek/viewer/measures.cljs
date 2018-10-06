(ns shevek.viewer.measures
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch] :refer-macros [defevh defevhi]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :refer [loading?]]
            [shevek.rpc :as rpc]
            [shevek.components.form :refer [checkbox toggle-checkbox-inside]]
            [shevek.domain.dimension :refer [add-dimension remove-dimension includes-dim?]]
            [shevek.viewer.shared :refer [current-cube viewer panel-header send-main-query description-help-icon]]
            [shevek.viewer.url :refer [store-viewer-in-url]]))

(defevhi :measure-toggled [db dim selected]
  {:after [store-viewer-in-url]}
  (let [set-viz-measures #(assoc-in % [:viewer :visualization :measures] (get-in % [:viewer :measures]))]
    (cond-> (update-in db [:viewer :measures] (if selected add-dimension remove-dimension) dim)
            selected send-main-query
            (not selected) set-viz-measures)))

(defn- measure-item [{:keys [name title] :as dim} selected-measures]
  [:div.item {:on-click toggle-checkbox-inside}
   [checkbox (str "cb-measure-" name)
    [:span title [description-help-icon dim]]
    {:checked (includes-dim? selected-measures dim) :on-change #(dispatch :measure-toggled dim %)}]])

(defn- measures-panel []
  (let [selected-measures (viewer :measures)]
    [:div.measures.panel.ui.basic.segment (rpc/loading-class :cube-metadata)
     [panel-header (t :viewer/measures)]
     [:div.items
      (for [m (sort-by :title (current-cube :measures))]
        ^{:key (:name m)} [measure-item m selected-measures])]]))
