(ns shevek.menu.fullscreen
  (:require [shevek.reflow.core :refer [dispatch] :refer-macros [defevh]]
            [shevek.reflow.db :as db]
            [shevek.i18n :refer [t]]))

(defevh :viewer/fullscreen-toggled [{:keys [viewer] :as db}]
  (update-in db [:viewer :fullscreen] not))

(defn fullscreen-menu []
  [:a.icon.item {:on-click #(dispatch :viewer/fullscreen-toggled)
                 :title (t :viewer/toggle-fullscreen)
                 :class (when (db/get-in [:viewer :fullscreen]) "active")}
   [:i.maximize.icon]])
