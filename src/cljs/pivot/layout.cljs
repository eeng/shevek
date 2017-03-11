(ns pivot.layout
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [pivot.i18n :refer [t]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [pivot.rpc :refer [loading?]]
            [pivot.dashboard :as dashboard]
            [pivot.settings :as settings]
            [pivot.cube :as cube]))

(defevh :navigate [db page & [params]]
  (assoc db :page page :params params))

(defroute home-path "/" []
  (dispatch :navigate #'dashboard/page))

(defroute settings-path "/settings" []
  (dispatch :navigate #'settings/page))

(defroute cube-path "/cubes/:cube" [cube]
  (dispatch :navigate #'cube/page {:cube-name cube}))

(defn active? [page]
  (when (= (db/get :page) page) "active"))

(defn layout []
  [:div
   [:div.ui.fixed.inverted.menu
    [:a.item {:href "#/" :class (active? #'dashboard/page)}
     [:i.block.layout.icon] (t :dashboard/title)]
    [:a.item {:href "#/cubes/vtol_stats" :class (active? #'cube/page)}
     [:i.cubes.icon] (t :cubes/menu)]
    [:div.right.menu
     (when (loading?) [:div.item [:i.spinner.loading.icon]])
     [:a.item {:href "#/settings" :class (active? #'settings/page)}
      [:i.settings.icon] (t :settings/title)]
     [:a.item {:href "#/logout"} [:i.sign.out.icon] (t :menu/logout)]]]
   [:div.page
    [(db/get :page :div)]]])
