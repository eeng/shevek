(ns shevek.menu.fullscreen
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]
            [shevek.components.popup :refer [tooltip]]))

(defevh :viewer/fullscreen-toggled [{:keys [viewer] :as db}]
  (update-in db [:viewer :fullscreen] not))

(defn fullscreen-menu []
  [:a.icon.item {:on-click #(dispatch :viewer/fullscreen-toggled)
                 :class (when (db/get-in [:viewer :fullscreen]) "active")
                 :ref (tooltip (t :viewer/toggle-fullscreen))}
   [:i.maximize.icon]])
