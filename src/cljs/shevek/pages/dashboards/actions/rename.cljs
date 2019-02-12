(ns shevek.pages.dashboards.actions.rename
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.components.editable-text :refer [editable-text]]
            [shevek.pages.dashboards.helpers :refer [modifiable?]]))

(defevh :dashboards/rename [db new-name]
  (assoc-in db [:current-dashboard :name] new-name))

(defn dashboard-name [{:keys [name] :as dashboard}]
  [:div.topbar-header
   (if (modifiable? dashboard)
     [editable-text {:text name :on-save #(dispatch :dashboards/rename %)}]
     name)])
