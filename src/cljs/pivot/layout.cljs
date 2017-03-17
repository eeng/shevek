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
            [pivot.cube-view.page :as cube]
            [pivot.cube-view.shared :refer [current-cube-name current-cube]]
            [pivot.dw :as dw]))

(def pages
  {:dashboard #'dashboard/page
   :settings #'settings/page
   :cube #'cube/page})

(defevh :navigate [db page]
  (assoc db :page page))

(defroute "/" []
  (dispatch :navigate :dashboard))

(defroute "/settings" []
  (dispatch :navigate :settings))

(defroute "/cubes/:cube" [cube]
  (dispatch :cube-selected cube))

; TODO faltan las rutas para errores

(defn active? [page]
  (when (= (db/get :page) page) "active"))

(defn cubes-menu []
  (let [cube-name (current-cube-name)
        cube-title (current-cube :title)]
    [make-dropdown {}
     [:div.ui.dropdown.item
      [:i.cubes.icon]
      [:div.text (if (active? :cube) cube-title (t :cubes/menu))]
      [:i.dropdown.icon]
      [:div.menu
       (for [{:keys [name title]} (dw/cubes-list)]
         ^{:key name}
         [:a.item {:href (str "#/cubes/" name)
                   :class (when (= cube-name name) "active")}
          title])]]]))

(defn layout []
  (dw/fetch-cubes)
  (fn []
    [:div
     [:div.ui.fixed.inverted.menu
      [:a.item {:href "#/" :class (active? :dashboard)}
       [:i.block.layout.icon] (t :dashboard/title)]
      [cubes-menu]
      [:div.right.menu
       (when (loading?) [:div.item [:i.repeat.loading.icon]])
       [:a.item {:href "#/settings" :class (active? :settings)}
        [:i.settings.icon] (t :settings/title)]
       [:a.item {:href "#/logout"} [:i.sign.out.icon] (t :menu/logout)]]]
     [:div.page
      [(get pages (db/get :page) :div)]]]))
