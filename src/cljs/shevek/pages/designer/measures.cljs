(ns shevek.pages.designer.measures
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.components.form :refer [checkbox toggle-checkbox-inside]]
            [shevek.pages.designer.helpers :refer [current-cube panel-header description-help-icon send-designer-query]]
            [shevek.reflow.db :as db]
            [shevek.domain.dimension :refer [add-dimension remove-dimension includes-dim?]]))

(defevh :designer/measure-toggled [db dim selected]
  (-> (update-in db [:designer :measures] (if selected add-dimension remove-dimension) dim)
      (send-designer-query)))

(defn- measure-item [{:keys [name title] :as dim} selected-measures]
  [:div.item {:on-click toggle-checkbox-inside}
   [checkbox (str "cb-measure-" name)
    [:span title [description-help-icon dim]]
    {:checked (includes-dim? selected-measures dim) :on-change #(dispatch :designer/measure-toggled dim %)}]])

(defn measures-panel [measures]
  [:div.measures.panel.ui.basic.segment
   [panel-header (t :designer/measures)]
   [:div.items
    (for [m (sort-by :title (current-cube :measures))]
      ^{:key (:name m)} [measure-item m measures])]])
