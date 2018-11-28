(ns shevek.menu.maximize
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]))

(defevh :viewer/maximized-toggled [{:keys [viewer] :as db}]
  (update-in db [:viewer :maximized] not))

(defn maximize-menu []
  (if (db/get-in [:viewer :maximized])
    [:a.icon.item {:on-click #(dispatch :viewer/maximized-toggled)
                   :class "active"
                   :ref (tooltip (t :viewer/minimize))}
     [:i.icon {:class "compress"}]]
    [:a.icon.item {:on-click #(dispatch :viewer/maximized-toggled)
                   :ref (tooltip (t :viewer/maximize))}
     [:i.icon {:class "expand"}]]))
