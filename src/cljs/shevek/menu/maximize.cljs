(ns shevek.menu.maximize
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]))

(defevh :designer/maximized-toggled [{:keys [designer] :as db}]
  (update-in db [:designer :maximized] not))

(defn maximize-menu []
  (if (db/get-in [:designer :maximized])
    [:a.icon.item {:on-click #(dispatch :designer/maximized-toggled)
                   :class "active"
                   :ref (tooltip (t :designer/minimize))}
     [:i.icon {:class "compress"}]]
    [:a.icon.item {:on-click #(dispatch :designer/maximized-toggled)
                   :ref (tooltip (t :designer/maximize))}
     [:i.icon {:class "expand"}]]))
