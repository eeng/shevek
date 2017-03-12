(ns pivot.layout
  (:require-macros [reflow.macros :refer [defevh]])
  (:require [pivot.i18n :refer [t]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [reflow.core :refer [dispatch]]
            [reflow.db :as db]
            [pivot.rpc :refer [loading?]]
            [pivot.components :refer [make-dropdown]]
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
  (dispatch :navigate #'cube/page {:selected-cube cube}))

(defn active? [page]
  (when (= (db/get :page) page) "active"))

(defn cubes-menu []
  [make-dropdown {}
   (let [cube-name (db/get-in [:params :selected-cube])
         cube-title (-> (filter #(= cube-name (:name %)) (db/get :cubes)) first :title)]
     [:div.ui.dropdown.item
      [:i.cubes.icon]
      [:div.text (or cube-title (t :cubes/menu))]
      [:i.dropdown.icon]
      [:div.menu
       (for [{:keys [name title]} (db/get :cubes)]
         ^{:key name}
         [:a.item {:href (str "#/cubes/" name) :class (when (= cube-name name) "active")} title])]])])

(defn layout []
  [:div
   [:div.ui.fixed.inverted.menu
    [:a.item {:href "#/" :class (active? #'dashboard/page)}
     [:i.block.layout.icon] (t :dashboard/title)]
    [cubes-menu]
    [:div.right.menu
     (when (loading?) [:div.item [:i.spinner.loading.icon]])
     [:a.item {:href "#/settings" :class (active? #'settings/page)}
      [:i.settings.icon] (t :settings/title)]
     [:a.item {:href "#/logout"} [:i.sign.out.icon] (t :menu/logout)]]]
   [:div.page
    [(db/get :page :div)]]])
