(ns shevek.viewer.measures
  (:require-macros [shevek.reflow.macros :refer [defevhi]])
  (:require [reagent.core :as r]
            [shevek.reflow.core :refer [dispatch]]
            [shevek.i18n :refer [t]]
            [shevek.rpc :refer [loading?]]
            [shevek.rpc :as rpc]
            [shevek.components.form :refer [checkbox toggle-checkbox-inside]]
            [shevek.lib.dw.dims :refer [add-dimension remove-dimension includes-dim?]]
            [shevek.viewer.shared :refer [current-cube viewer panel-header send-main-query]]
            [shevek.reports.url :refer [store-viewer-in-url]]))

(defevhi :measure-toggled [db dim selected]
  {:after [store-viewer-in-url]}
  (cond-> (update-in db [:viewer :measures] (if selected add-dimension remove-dimension) dim)
          selected (send-main-query)))

(defn- measure-item [{:keys [name title description] :as dim} selected-measures]
  [:div.item {:on-click toggle-checkbox-inside :title description}
   [checkbox (str "cb-measure-" name) title
    {:checked (includes-dim? selected-measures dim) :on-change #(dispatch :measure-toggled dim %)}]])

(defn- measures-panel []
  (let [selected-measures (viewer :measures)]
    [:div.measures.panel.ui.basic.segment (rpc/loading-class :cube-metadata)
     [panel-header (t :cubes/measures)]
     [:div.items
      (for [m (sort-by :title (current-cube :measures))]
        ^{:key (:name m)} [measure-item m selected-measures])]]))
