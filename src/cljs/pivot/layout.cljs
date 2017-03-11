(ns pivot.layout
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [pivot.i18n :refer [t]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [pivot.dashboard :as dashboard]
            [pivot.settings :as settings]))

(defevh :navigate [db page]
  (assoc db :page page))

(defroute home-path "/" []
  (dispatch :navigate #'dashboard/page))

(defroute settings-path "/settings" []
  (dispatch :navigate #'settings/page))

(defn layout []
  [:div
   [:div.ui.fixed.inverted.menu
    [:div.ui.container
      [:a.item {:href "/#/"} [:i.block.layout.icon] (t :dashboard/menu)]
      [:a.item {:href "/#/"} [:i.cubes.icon] (t :cubes/menu)]
      [:div.right.menu
       [:a.item {:href "/#/settings"} [:i.settings.icon] (t :menu/settings)]
       [:a.item {:href "/#/logout"} [:i.sign.out.icon] (t :menu/logout)]]]]
   [:div.ui.page.container
    [(db/get :page #'dashboard/page)]]])
