(ns shevek.pages.designer.measures
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.i18n :refer [t]]
            [shevek.components.form :refer [checkbox]]
            [shevek.pages.designer.helpers :refer [current-cube panel-header description-help-icon send-designer-query]]
            [shevek.reflow.db :as db]
            [shevek.domain.dimension :refer [add-dimension remove-dimension includes-dim?]]))

(defevh :designer/measure-toggled [db dim selected]
  (-> (update-in db [:designer :measures] (if selected add-dimension remove-dimension) dim)
      (send-designer-query)))

(defn- measure-item [{:keys [name title] :as dim} selected-measures]
  (let [checked (includes-dim? selected-measures dim)
        on-measure-toggled #(dispatch :designer/measure-toggled dim (not checked))]
    [:div.item {:on-click on-measure-toggled}
     [checkbox (str "cb-measure-" name)
      [:span title [description-help-icon dim]]
      {:checked checked :on-change on-measure-toggled}]]))

(defn measures-panel [measures]
  [:div.measures.panel.ui.basic.segment
   [panel-header (t :designer/measures)]
   [:div.items
    (for [m (sort-by :title (current-cube :measures))]
      ^{:key (:name m)} [measure-item m measures])]])
