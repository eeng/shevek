(ns shevek.pages.designer.actions.maximize
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]))

(defevh :designer/maximized-toggled [{:keys [designer] :as db}]
  (update-in db [:designer :maximized] not))

(defn maximize-button []
  (let [maximized? (db/get-in [:designer :maximized])]
    [:button.ui.default.icon.button
     {:on-click #(dispatch :designer/maximized-toggled)
      :class "active"
      :ref (tooltip (t (if maximized? :designer/minimize :designer/maximize)))}
     [:i.icon {:class (if maximized? "compress" "expand")}]]))
