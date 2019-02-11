(ns shevek.pages.dashboards.actions.rename
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.editable-text :refer [editable-text]]))

(defevh :dashboards/rename [db new-name]
  (assoc-in db [:current-dashboard :name] new-name))

(defn dashboard-name [{:keys [name]}]
  [:div.topbar-header
    [editable-text {:text name :on-save #(dispatch :dashboards/rename %)}]])
